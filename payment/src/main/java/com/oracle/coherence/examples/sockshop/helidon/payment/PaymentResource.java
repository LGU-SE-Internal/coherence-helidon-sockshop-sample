/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.payment;

import java.util.logging.Logger;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Implementation of the Payment Service REST API.
 */
@ApplicationScoped
@Path("/payments")
public class PaymentResource implements PaymentApi {
    private static final Logger LOGGER = Logger.getLogger(PaymentResource.class.getName());

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

    @Override
    @WithSpan
    public Response getOrderAuthorizations(String orderId) {
        LOGGER.info("Getting authorizations for order: " + orderId);
        return Response.ok(payments.findAuthorizationsByOrder(orderId)).build();
    }

    @Override
    @WithSpan
    public Authorization authorize(PaymentRequest paymentRequest) {
        String firstName = paymentRequest.getCustomer().getFirstName();
        String lastName  = paymentRequest.getCustomer().getLastName();
        String orderId = paymentRequest.getOrderId();

        LOGGER.info("Authorizing payment for order: " + orderId + ", customer: " + firstName + " " + lastName);

        try {
            Authorization auth = paymentService.authorize(
                    orderId,
                    firstName,
                    lastName,
                    paymentRequest.getCard(),
                    paymentRequest.getAddress(),
                    paymentRequest.getAmount());

            payments.saveAuthorization(auth);

            if (!auth.isAuthorised()) {
                LOGGER.warning("Payment declined for order: " + orderId + ", reason: " + auth.getMessage());
            } else {
                LOGGER.info("Payment authorized for order: " + orderId);
            }

            return auth;
        } catch (Exception e) {
            LOGGER.severe("Error authorizing payment for order: " + orderId + ", error: " + e.getMessage());
            throw e;
        }
    }


}
