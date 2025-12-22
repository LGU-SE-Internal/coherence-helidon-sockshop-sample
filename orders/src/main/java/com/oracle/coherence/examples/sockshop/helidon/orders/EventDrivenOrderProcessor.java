/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.helidon.grpc.api.Grpc;
import io.helidon.tracing.Span;
import io.helidon.tracing.Tracer;
import io.helidon.tracing.Scope;
import io.helidon.tracing.HeaderProvider;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.Updated;
import com.tangosol.net.events.partition.cache.EntryEvent;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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
        String traceParent = TraceUtils.injectCurrentTraceParent();
        order.setTraceParent(traceParent);
        
        if (TraceUtils.hasTraceContext(traceParent)) {
            log.info("Injected trace context into order {}: {}", 
                     order.getOrderId(), traceParent);
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
        String traceParent = order.getTraceParent();
        
        if (TraceUtils.hasTraceContext(traceParent)) {
            log.info("Processing order with trace context {}: {}", 
                     order.getOrderId(), traceParent);
        } else {
            log.warn("No trace context found for order {}, creating new trace", order.getOrderId());
        }
        
        // Get Helidon's global tracer
        Tracer tracer = Tracer.global();
        
        // Extract parent context using Helidon's built-in parser
        Optional<io.helidon.tracing.SpanContext> parentContext = Optional.empty();
        if (TraceUtils.hasTraceContext(traceParent)) {
            // Create a HeaderProvider that provides the traceparent header
            final String finalTraceParent = traceParent;
            HeaderProvider headerProvider = new HeaderProvider() {
                @Override
                public Iterable<String> keys() {
                    return java.util.List.of("traceparent");
                }
                
                @Override
                public Optional<String> get(String key) {
                    if ("traceparent".equalsIgnoreCase(key)) {
                        return Optional.of(finalTraceParent);
                    }
                    return Optional.empty();
                }
                
                @Override
                public Iterable<String> getAll(String key) {
                    if ("traceparent".equalsIgnoreCase(key)) {
                        return java.util.List.of(finalTraceParent);
                    }
                    return java.util.List.of();
                }
                
                @Override
                public boolean contains(String key) {
                    return "traceparent".equalsIgnoreCase(key);
                }
            };
            
            // Use Helidon's extract method to parse the traceparent
            parentContext = tracer.extract(headerProvider);
            if (parentContext.isPresent()) {
                log.info("Extracted parent context for order {}: traceId={}", 
                         order.getOrderId(), parentContext.get().traceId());
            }
        }
        
        // Create a span using Helidon API with explicit parent context
        Span.Builder<?> spanBuilder = tracer.spanBuilder("process-order-event")
                .tag("order.id", order.getOrderId())
                .tag("order.status", order.getStatus().toString());
        
        // Explicitly set parent context if available
        parentContext.ifPresent(spanBuilder::parent);
        
        Span asyncSpan = spanBuilder.start();
        
        try (Scope scope = asyncSpan.activate()) {
            log.info("Processing order event for order: {} with status: {}", 
                     order.getOrderId(), order.getStatus());
            
            // Save original trace context to propagate to next async event
            String originalTraceParent = traceParent;
            
            switch (order.getStatus()) {
            case CREATED:
                try {
                    processPayment(order);
                }
                finally {
                    // Restore original trace context before saving (don't create cascading chain)
                    order.setTraceParent(originalTraceParent);
                    saveOrder(order);
                }
                break;

            case PAID:
                try {
                    shipOrder(order);
                }
                finally {
                    // Restore original trace context before saving
                    order.setTraceParent(originalTraceParent);
                    saveOrder(order);
                }
                break;

            default:
                // do nothing, order is in a terminal state already
            }
            
            asyncSpan.status(Span.Status.OK);
        } catch (Exception e) {
            log.error("Error processing order event for order: " + order.getOrderId(), e);
            asyncSpan.status(Span.Status.ERROR);
            throw e;
        } finally {
            asyncSpan.end();
        }
    }
}
