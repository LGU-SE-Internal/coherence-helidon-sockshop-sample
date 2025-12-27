/*
 * Copyright (c) 2020,2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.carts;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the Cart Service REST API.
 */
@ApplicationScoped
@Path("/carts")
@Slf4j
public class CartResource implements CartApi {

    @Inject
    private CartRepository carts;

    @Override
    public Cart getCart(String customerId) {
        log.info("Getting cart for customer: " + customerId);
        return carts.getOrCreateCart(customerId);
    }

    @Override
    public Response deleteCart(String customerId) {
        log.info("Deleting cart for customer: " + customerId);
        boolean deleted = carts.deleteCart(customerId);
        if (!deleted) {
            log.warn("Cart not found for customer: " + customerId);
        }
        return deleted ?
                Response.accepted().build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @Override
    public Response mergeCarts(String customerId, String sessionId) {
        log.info("Merging carts for customer: " + customerId + ", session: " + sessionId);
        boolean fMerged = carts.mergeCarts(customerId, sessionId);
        if (!fMerged) {
            log.debug("No session cart to merge for customer: " + customerId + " (session: " + sessionId + ")");
        }
        return fMerged
                ? Response.accepted().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }

    @Override
    public ItemsApi getItems(String customerId) {
        log.info("Getting items for customer: " + customerId);
        return new ItemsResource(carts, customerId);
    }
}
