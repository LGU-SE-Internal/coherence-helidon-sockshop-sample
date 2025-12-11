/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.shipping;

import io.grpc.ServerInterceptor;
import io.helidon.grpc.api.Grpc;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Provider for OpenTelemetry gRPC server interceptor.
 * This interceptor adds tracing capabilities to incoming gRPC calls.
 */
@ApplicationScoped
public class GrpcServerInterceptorProvider {

    /**
     * Produces a gRPC server interceptor that adds OpenTelemetry tracing to server calls.
     *
     * @return the server interceptor
     */
    @Produces
    @Grpc.GrpcInterceptor
    public ServerInterceptor createTracingInterceptor() {
        GrpcTelemetry grpcTelemetry = GrpcTelemetry.create(GlobalOpenTelemetry.get());
        return grpcTelemetry.newServerInterceptor();
    }
}
