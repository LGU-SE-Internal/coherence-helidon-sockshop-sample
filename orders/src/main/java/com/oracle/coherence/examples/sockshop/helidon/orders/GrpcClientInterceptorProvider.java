/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.grpc.ClientInterceptor;
import io.helidon.grpc.api.Grpc;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Provider for OpenTelemetry gRPC client interceptor.
 * This interceptor adds tracing capabilities to outgoing gRPC calls.
 */
@ApplicationScoped
public class GrpcClientInterceptorProvider {

    /**
     * Produces a gRPC client interceptor that adds OpenTelemetry tracing to client calls.
     *
     * @return the client interceptor
     */
    @Produces
    @Grpc.GrpcInterceptor
    public ClientInterceptor createTracingInterceptor() {
        GrpcTelemetry grpcTelemetry = GrpcTelemetry.create(GlobalOpenTelemetry.get());
        return grpcTelemetry.newClientInterceptor();
    }
}
