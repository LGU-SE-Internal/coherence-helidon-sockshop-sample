/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.logging.Logger;

/**
 * CDI Producer to explicitly create and expose the TracePropagationClientInterceptor bean.
 * This ensures the interceptor is properly discovered by Helidon's gRPC client infrastructure.
 */
@ApplicationScoped
public class InterceptorProducer {

    private static final Logger LOGGER = Logger.getLogger(InterceptorProducer.class.getName());

    @Produces
    @ApplicationScoped
    public TracePropagationClientInterceptor tracingInterceptor() {
        LOGGER.info(">>>> [INTERCEPTOR PRODUCER] Creating TracePropagationClientInterceptor bean");
        return new TracePropagationClientInterceptor();
    }
}
