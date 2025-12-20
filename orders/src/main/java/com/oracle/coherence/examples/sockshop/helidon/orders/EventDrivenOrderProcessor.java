/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.helidon.grpc.api.Grpc;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.Updated;
import com.tangosol.net.events.partition.cache.EntryEvent;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
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
        // Inject current trace context into the order before saving
        // This allows trace propagation across async event boundaries
        injectTraceContext(order);
        saveOrder(order);
    }
    
    // ---- helpers ---------------------------------------------------------
    
    /**
     * Inject OpenTelemetry trace context into Order object for async propagation.
     *
     * @param order the order to inject trace context into
     */
    private void injectTraceContext(Order order) {
        Context current = Context.current();
        Map<String, String> carrier = new HashMap<>();
        
        // Use B3 propagator to inject trace context (compatible with most systems)
        B3Propagator.injectingMultiHeaders().inject(current, carrier, new TextMapSetter<Map<String, String>>() {
            @Override
            public void set(Map<String, String> carrier, String key, String value) {
                if (carrier != null) {
                    carrier.put(key, value);
                }
            }
        });
        
        // Store the traceparent in the order for later extraction
        if (carrier.containsKey("X-B3-TraceId")) {
            String traceId = carrier.get("X-B3-TraceId");
            String spanId = carrier.getOrDefault("X-B3-SpanId", "");
            String sampled = carrier.getOrDefault("X-B3-Sampled", "1");
            order.setTraceParent(traceId + "-" + spanId + "-" + sampled);
            log.debug("Injected trace context into order {}: {}", order.getOrderId(), order.getTraceParent());
        }
    }
    
    /**
     * Extract OpenTelemetry trace context from Order object.
     *
     * @param order the order to extract trace context from
     * @return the restored context, or root context if no trace info available
     */
    private Context extractTraceContext(Order order) {
        String traceParent = order.getTraceParent();
        
        if (traceParent == null || traceParent.isEmpty()) {
            log.debug("No trace context found in order {}", order.getOrderId());
            return Context.root();
        }
        
        // Parse the stored trace context (format: traceId-spanId-sampled)
        String[] parts = traceParent.split("-");
        if (parts.length < 2) {
            log.warn("Invalid trace context format in order {}: {}", order.getOrderId(), traceParent);
            return Context.root();
        }
        
        Map<String, String> carrier = new HashMap<>();
        carrier.put("X-B3-TraceId", parts[0]);
        carrier.put("X-B3-SpanId", parts[1]);
        if (parts.length > 2) {
            carrier.put("X-B3-Sampled", parts[2]);
        }
        
        // Extract context using B3 propagator
        Context context = B3Propagator.injectingMultiHeaders().extract(Context.root(), carrier, new TextMapGetter<Map<String, String>>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier != null ? carrier.keySet() : Collections.emptyList();
            }
            
            @Override
            public String get(Map<String, String> carrier, String key) {
                return carrier != null ? carrier.get(key) : null;
            }
        });
        
        log.debug("Extracted trace context from order {}: traceId={}", order.getOrderId(), parts[0]);
        return context;
    }

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
        
        // Extract and restore the trace context from the Order object
        // This propagates the trace across the async boundary
        Context parentContext = extractTraceContext(order);
        
        try (Scope scope = parentContext.makeCurrent()) {
            String traceId = Span.current().getSpanContext().getTraceId();
            log.info("Processing order event for order: {} with status: {} (traceId: {})", 
                     order.getOrderId(), order.getStatus(), traceId);
            
            switch (order.getStatus()) {
            case CREATED:
                try {
                    processPayment(order);
                }
                finally {
                    saveOrder(order);
                }
                break;

            case PAID:
                try {
                    shipOrder(order);
                }
                finally {
                    saveOrder(order);
                }
                break;

            default:
                // do nothing, order is in a terminal state already
            }
        } catch (Exception e) {
            log.error("Error processing order event for order: " + order.getOrderId(), e);
            // Record exception in current span if available
            Span.current().recordException(e);
            throw e;
        }
    }
}
