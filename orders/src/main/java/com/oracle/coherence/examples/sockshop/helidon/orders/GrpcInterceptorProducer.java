/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.grpc.ClientInterceptor;
import io.helidon.grpc.api.Grpc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI producer for gRPC client interceptors.
 */
@ApplicationScoped
public class GrpcInterceptorProducer {
    
    /**
     * Produces the trace propagation interceptor for all gRPC clients.
     *
     * @return trace propagation interceptor
     */
    @Produces
    @Grpc.GrpcInterceptor
    public ClientInterceptor produceTracePropagationInterceptor() {
        return new TracePropagationInterceptor();
    }
}
