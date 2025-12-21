/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.users;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

import static com.oracle.coherence.examples.sockshop.helidon.users.JsonHelpers.embed;
import static com.oracle.coherence.examples.sockshop.helidon.users.JsonHelpers.obj;

@ApplicationScoped
@Path("/customers")
@Slf4j
public class CustomersResource implements CustomerApi {

    @Inject
    private UserRepository users;

    @Override
    public Response getAllCustomers() {
        log.info("Getting all customers");
        return Response.ok(embed("customer", users.getAllUsers())).build();
    }

    @Override
    public Response getCustomer(String id) {
        log.info("Getting customer: " + id);
        return Response.ok(users.getOrCreate(id)).build();
    }

    @Override
    public Response deleteCustomer(String id) {
        log.info("Deleting customer: " + id);
        User prev = users.removeUser(id);
        if (prev == null) {
            log.warn("Customer not found for deletion: " + id);
        }
        return Response.ok(obj().add("status", prev != null).build()).build();
    }

    @Override
    public Response getCustomerCards(String id) {
        log.info("Getting cards for customer: " + id);
        User user = users.getUser(id);
        return Response.ok(embed("card", user.getCards().stream().map(Card::mask).toArray())).build();
    }

    @Override
    public Response getCustomerAddresses(String id) {
        log.info("Getting addresses for customer: " + id);
        User user = users.getUser(id);
        return Response.ok(embed("address", user.getAddresses())).build();
    }
}
