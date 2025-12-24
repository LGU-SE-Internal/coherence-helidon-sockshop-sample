/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.grpc.ClientInterceptor;
import io.helidon.webclient.grpc.tracing.GrpcClientTracing;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * Bridge class to integrate Helidon's official gRPC client tracing interceptor.
 * <p>
 * This class extracts the official {@link io.helidon.webclient.grpc.tracing.GrpcClientTracingInterceptor}
 * from Helidon's {@link GrpcClientTracing} service and makes it available as a gRPC client interceptor.
 * The interceptor automatically injects OpenTelemetry trace context into outgoing gRPC calls.
 * </p>
 */
@ApplicationScoped
@Priority(1) // Execute before other interceptors
@Slf4j
public class GrpcClientTracingBridge implements ClientInterceptor {

    private final ClientInterceptor delegate;

    public GrpcClientTracingBridge() {
        log.info("Initializing GrpcClientTracingBridge");
        
        // Create the official Helidon gRPC client tracing service
        // Pass empty config - the GrpcClientTracing doesn't actually need configuration
        // as it uses the global Tracer automatically
        GrpcClientTracing tracingService = GrpcClientTracing.create(
                io.helidon.config.Config.empty());
        
        // Extract the official tracing interceptor from the service
        // The GrpcClientTracing.interceptors() returns a WeightedBag containing
        // the GrpcClientTracingInterceptor
        this.delegate = tracingService.interceptors().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to obtain GrpcClientTracingInterceptor from GrpcClientTracing service"));
        
        log.info("GrpcClientTracingBridge initialized with delegate: {}", delegate.getClass().getName());
    }

    @Override
    public <ReqT, RespT> io.grpc.ClientCall<ReqT, RespT> interceptCall(
            io.grpc.MethodDescriptor<ReqT, RespT> method,
            io.grpc.CallOptions callOptions,
            io.grpc.Channel next) {
        log.debug("Intercepting gRPC client call: {}", method.getFullMethodName());
        return delegate.interceptCall(method, callOptions, next);
    }
}
