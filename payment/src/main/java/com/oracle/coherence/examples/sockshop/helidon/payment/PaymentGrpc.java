/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.payment;

import io.helidon.grpc.api.Grpc;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import java.util.Collection;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.metrics.annotation.Counted;

/**
 * Implementation of the Payment Service gRPC API.
 */
@ApplicationScoped
@Grpc.GrpcService("PaymentGrpc")
@Grpc.GrpcMarshaller("jsonb")
@Slf4j
public class PaymentGrpc {
    /**
     * Payment repository to use.
     */
    @Inject
    private PaymentRepository payments;

    /**
     * Payment service to use.
     */
    @Inject
    private PaymentService paymentService;

    @Grpc.Unary
    public Collection<? extends Authorization> getOrderAuthorizations(String orderId) {
        return payments.findAuthorizationsByOrder(orderId);
    }

    @Grpc.Unary
    @Counted
    public Authorization authorize(PaymentRequest paymentRequest) {
        String tp = paymentRequest.getTraceParent();
        // log.info(">>>> [RELAY RECEIVE] Received payment Trace: {}", tp);

        io.helidon.tracing.Tracer tracer = io.helidon.tracing.Tracer.global();
        io.helidon.tracing.Span.Builder<?> spanBuilder = tracer.spanBuilder("PaymentGrpc/authorize")
                .kind(io.helidon.tracing.Span.Kind.SERVER);

        // Use HeaderProvider to extract parent context from traceParent field
        if (tp != null && !tp.isEmpty()) {
            io.helidon.tracing.HeaderProvider hp = new io.helidon.tracing.HeaderProvider() {
                @Override 
                public java.util.Optional<String> get(String key) { 
                    return "traceparent".equals(key) ? java.util.Optional.of(tp) : java.util.Optional.empty(); 
                }
                
                @Override 
                public Iterable<String> keys() { 
                    return java.util.List.of("traceparent"); 
                }
                
                @Override
                public Iterable<String> getAll(String key) {
                    if ("traceparent".equals(key)) {
                        return java.util.List.of(tp);
                    }
                    return java.util.List.of();
                }
                
                @Override 
                public boolean contains(String key) { 
                    return "traceparent".equals(key); 
                }
            };
            
            // Force parent context linkage
            tracer.extract(hp).ifPresent(spanBuilder::parent);
        }

        io.helidon.tracing.Span serverSpan = spanBuilder.start();
        try (io.helidon.tracing.Scope scope = serverSpan.activate()) {
            // Execute business logic within traced context
            String firstName = paymentRequest.getCustomer().getFirstName();
            String lastName  = paymentRequest.getCustomer().getLastName();

            Authorization auth = paymentService.authorize(
                    paymentRequest.getOrderId(),
                    firstName,
                    lastName,
                    paymentRequest.getCard(),
                    paymentRequest.getAddress(),
                    paymentRequest.getAmount());

            payments.saveAuthorization(auth);

            serverSpan.status(io.helidon.tracing.Span.Status.OK);
            return auth;
        } catch (Exception e) {
            log.error("Error authorizing payment", e);
            serverSpan.status(io.helidon.tracing.Span.Status.ERROR);
            serverSpan.addEvent("exception", 
                java.util.Map.of(
                    "exception.type", e.getClass().getName(),
                    "exception.message", e.getMessage() != null ? e.getMessage() : "No message available"
                ));
            throw e;
        } finally {
            serverSpan.end();
        }
    }
}
