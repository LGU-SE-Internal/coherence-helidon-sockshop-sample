/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.shipping;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.helidon.grpc.api.Grpc;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * gRPC server interceptor that extracts OpenTelemetry trace context from gRPC metadata.
 * This enables distributed tracing across gRPC calls in Helidon 4, which is not
 * automatically instrumented by the OpenTelemetry Java Agent.
 */
@ApplicationScoped
@Grpc.GrpcInterceptor
public class GrpcServerTraceInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        // Extract trace context from gRPC metadata
        Context extractedContext = GlobalOpenTelemetry.getPropagators().getTextMapPropagator().extract(
            Context.root(),
            headers,
            GrpcTraceUtils.METADATA_TEXT_MAP_GETTER
        );

        // Execute the call with the extracted context
        try (Scope scope = extractedContext.makeCurrent()) {
            return next.startCall(call, headers);
        }
    }
}
