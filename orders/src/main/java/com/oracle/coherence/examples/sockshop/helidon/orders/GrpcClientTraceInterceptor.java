/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.helidon.grpc.api.Grpc;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * gRPC client interceptor that injects OpenTelemetry trace context into gRPC metadata.
 * This enables distributed tracing across gRPC calls in Helidon 4, which is not
 * automatically instrumented by the OpenTelemetry Java Agent.
 */
@ApplicationScoped
@Grpc.GrpcInterceptor
public class GrpcClientTraceInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Inject the current OpenTelemetry context into gRPC Metadata
                GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(
                    Context.current(),
                    headers,
                    (carrier, key, value) -> {
                        Metadata.Key<String> metadataKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                        carrier.put(metadataKey, value);
                    }
                );
                super.start(responseListener, headers);
            }
        };
    }
}
