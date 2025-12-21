/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.helidon.grpc.api.Grpc;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.Updated;
import com.tangosol.net.events.partition.cache.EntryEvent;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.oracle.coherence.examples.sockshop.helidon.orders.Order.Status.PAID;
import static com.oracle.coherence.examples.sockshop.helidon.orders.Order.Status.PAYMENT_FAILED;
import static com.oracle.coherence.examples.sockshop.helidon.orders.Order.Status.SHIPPED;

/**
 * A more realistic implementation of {@link OrderProcessor} that stores
 * submitted order immediately and uses Coherence server-side events
 * to process payment and ship the order asynchronously, based on the
 * order status.
 */
@Slf4j
@ApplicationScoped
public class EventDrivenOrderProcessor implements OrderProcessor {
    /**
     * Order repository to use.
     */
    @Inject
    protected OrderRepository orders;

    /**
     * Shipping service client.
     */
    @Inject
    @Grpc.GrpcProxy
    protected ShippingClient shippingService;

    /**
     * Payment service client.
     */
    @Inject
    @Grpc.GrpcProxy
    protected PaymentClient paymentService;

    // --- OrderProcessor interface -----------------------------------------

    @Override
    @WithSpan
    public void processOrder(Order order) {
        // Inject current trace context into order for async propagation
        Map<String, String> traceContext = TraceUtils.injectCurrentContext();
        order.setTraceContext(traceContext);
        
        if (TraceUtils.hasTraceContext(traceContext)) {
            log.info("Injected trace context into order {}: {}", 
                     order.getOrderId(), traceContext.get("traceparent"));
        }
        
        saveOrder(order);
    }
    // ---- helpers ---------------------------------------------------------

    /**
     * Save specified order.
     *
     * @param order the order to save
     */
    @WithSpan
    protected void saveOrder(Order order) {
        orders.saveOrder(order);
        log.info("Order saved: " + order);
    }

    /**
     * Process payment and update order with payment details.
     *
     * @param order the order to process the payment for
     *
     * @throws PaymentDeclinedException if the payment was declined
     */
    @WithSpan
    protected void processPayment(Order order) {
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getOrderId())
                .customer(order.getCustomer())
                .address(order.getAddress())
                .card(order.getCard())
                .amount(order.getTotal())
                .build();

        log.info("Processing Payment: " + paymentRequest);
        Payment payment = paymentService.authorize(paymentRequest);
        if (payment == null) {
            payment = Payment.builder()
                    .authorised(false)
                    .message("Unable to parse authorization packet")
                    .build();
        }
        log.info("Payment processed: " + payment);

        order.setPayment(payment);
        if (!payment.isAuthorised()) {
            order.setStatus(PAYMENT_FAILED);
            throw new PaymentDeclinedException(payment.getMessage());
        }

        order.setStatus(PAID);
    }

    /**
     * Submits order for shipping and updates order with shipment details.
     *
     * @param order the order to ship
     */
    @WithSpan
    protected void shipOrder(Order order) {
        ShippingRequest shippingRequest = ShippingRequest.builder()
                .orderId(order.getOrderId())
                .customer(order.getCustomer())
                .address(order.getAddress())
                .itemCount(order.getItems().size())
                .build();

        log.info("Creating Shipment: " + shippingRequest);
        Shipment shipment = shippingService.ship(shippingRequest);
        log.info("Created Shipment: " + shipment);

        order.setShipment(shipment);
        order.setStatus(SHIPPED);
    }

    // ---- helper methods --------------------------------------------------

    /**
     * An exception that is thrown if the payment is declined.
     */
    public static class PaymentDeclinedException extends OrderException {
        public PaymentDeclinedException(String s) {
            super(s);
        }
    }

    void onOrderCreated(@ObservesAsync @Inserted @Updated @MapName("orders") EntryEvent<String, Order> event) {
        Order order = event.getValue();
        Map<String, String> traceContext = order.getTraceContext();
        
        // Extract parent context from order
        Context parentContext = TraceUtils.extractContext(traceContext);
        
        if (TraceUtils.hasTraceContext(traceContext)) {
            log.info("Extracted trace context for order {}: {}", 
                     order.getOrderId(), traceContext.get("traceparent"));
        } else {
            log.warn("No trace context found for order {}, creating new trace", order.getOrderId());
        }
        
        // Create a new span as child of the restored context
        Tracer tracer = GlobalOpenTelemetry.getTracer("orders-async");
        Span asyncSpan = tracer.spanBuilder("process-order-event")
                .setParent(parentContext)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("order.id", order.getOrderId())
                .setAttribute("order.status", order.getStatus().toString())
                .startSpan();
        
        try (Scope scope = asyncSpan.makeCurrent()) {
            log.info("Processing order event for order: {} with status: {} (traceId: {})", 
                     order.getOrderId(), order.getStatus(), asyncSpan.getSpanContext().getTraceId());
            
            // Save original trace context to propagate to next async event
            Map<String, String> originalTraceContext = traceContext;
            
            switch (order.getStatus()) {
            case CREATED:
                try {
                    processPayment(order);
                }
                finally {
                    // Restore original trace context before saving (don't create cascading chain)
                    order.setTraceContext(originalTraceContext);
                    saveOrder(order);
                }
                break;

            case PAID:
                try {
                    shipOrder(order);
                }
                finally {
                    // Restore original trace context before saving
                    order.setTraceContext(originalTraceContext);
                    saveOrder(order);
                }
                break;

            default:
                // do nothing, order is in a terminal state already
            }
            
            asyncSpan.setStatus(StatusCode.OK);
        } catch (Exception e) {
            log.error("Error processing order event for order: " + order.getOrderId(), e);
            asyncSpan.recordException(e);
            asyncSpan.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            asyncSpan.end();
        }
    }
}
