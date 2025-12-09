/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.payment;

import io.helidon.grpc.api.Grpc;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import java.util.Collection;

import org.eclipse.microprofile.metrics.annotation.Counted;

/**
 * Implementation of the Payment Service gRPC API.
 */
@ApplicationScoped
@Grpc.GrpcService("PaymentGrpc")
@Grpc.GrpcMarshaller("jsonb")
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

    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("PaymentGrpc");

    @Grpc.Unary
    public Collection<? extends Authorization> getOrderAuthorizations(String orderId) {
        Span span = tracer.spanBuilder("PaymentGrpc.getOrderAuthorizations")
                .setAttribute("orderId", orderId)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            return payments.findAuthorizationsByOrder(orderId);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Grpc.Unary
    @Counted
    public Authorization authorize(PaymentRequest paymentRequest) {
        String firstName = paymentRequest.getCustomer().getFirstName();
        String lastName  = paymentRequest.getCustomer().getLastName();
        String orderId = paymentRequest.getOrderId();

        Span span = tracer.spanBuilder("PaymentGrpc.authorize")
                .setAttribute("orderId", orderId)
                .setAttribute("customer.firstName", firstName)
                .setAttribute("customer.lastName", lastName)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            Authorization auth = paymentService.authorize(
                    orderId,
                    firstName,
                    lastName,
                    paymentRequest.getCard(),
                    paymentRequest.getAddress(),
                    paymentRequest.getAmount());

            payments.saveAuthorization(auth);

            span.setAttribute("authorized", auth.isAuthorised());
            return auth;
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
