/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.shipping;

import io.grpc.ForwardingServerCallListener;
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
 * 
 * The interceptor wraps the listener to ensure the trace context is active during
 * all callback methods (onMessage, onHalfClose, etc.), not just during interceptCall.
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

        // Get the delegate listener
        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        // Wrap the listener to ensure context is active during all callbacks
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                try (Scope scope = extractedContext.makeCurrent()) {
                    super.onMessage(message);
                }
            }

            @Override
            public void onHalfClose() {
                try (Scope scope = extractedContext.makeCurrent()) {
                    super.onHalfClose();
                }
            }

            @Override
            public void onCancel() {
                try (Scope scope = extractedContext.makeCurrent()) {
                    super.onCancel();
                }
            }

            @Override
            public void onComplete() {
                try (Scope scope = extractedContext.makeCurrent()) {
                    super.onComplete();
                }
            }

            @Override
            public void onReady() {
                try (Scope scope = extractedContext.makeCurrent()) {
                    super.onReady();
                }
            }
        };
    }
}
