/*
 * Copyright (c) 2020,2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.carts;

import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Implementation of the Cart Service REST API.
 */
@ApplicationScoped
@Path("/carts")
public class CartResource implements CartApi {
    private static final Logger LOGGER = Logger.getLogger(CartResource.class.getName());

    @Inject
    private CartRepository carts;

    @Override
    public Cart getCart(String customerId) {
        LOGGER.info("Getting cart for customer: " + customerId);
        return carts.getOrCreateCart(customerId);
    }

    @Override
    public Response deleteCart(String customerId) {
        LOGGER.info("Deleting cart for customer: " + customerId);
        boolean deleted = carts.deleteCart(customerId);
        if (!deleted) {
            LOGGER.warning("Cart not found for customer: " + customerId);
        }
        return deleted ?
                Response.accepted().build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @Override
    public Response mergeCarts(String customerId, String sessionId) {
        LOGGER.info("Merging carts for customer: " + customerId + ", session: " + sessionId);
        boolean fMerged = carts.mergeCarts(customerId, sessionId);
        if (!fMerged) {
            LOGGER.warning("Failed to merge carts for customer: " + customerId);
        }
        return fMerged
                ? Response.accepted().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }

    @Override
    public ItemsApi getItems(String customerId) {
        LOGGER.info("Getting items for customer: " + customerId);
        return new ItemsResource(carts, customerId);
    }
}
