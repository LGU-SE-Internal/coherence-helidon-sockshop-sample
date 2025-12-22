/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.payment;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC server interceptor to log metadata headers for debugging trace propagation issues.
 * Runs with high priority to ensure it executes before OpenTelemetry interceptors.
 */
@ApplicationScoped
@io.helidon.grpc.api.Grpc.GrpcInterceptor
@Priority(1) // Execute before OTel interceptors
@Slf4j
public class MetadataLogger implements ServerInterceptor {
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        log.info("=== gRPC Request Metadata for {} ===", call.getMethodDescriptor().getFullMethodName());
        
        // Log all metadata headers
        for (String key : headers.keys()) {
            if (key.endsWith("-bin")) {
                // Binary headers
                log.info("  Header (binary): {}", key);
            } else {
                // Text headers
                Metadata.Key<String> metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                String value = headers.get(metaKey);
                log.info("  Header: {} = {}", key, value);
                
                // Specifically highlight trace-related headers
                if (key.equalsIgnoreCase("traceparent") || 
                    key.equalsIgnoreCase("tracestate") ||
                    key.equalsIgnoreCase("grpc-trace-bin")) {
                    log.warn("  >>> Trace header found: {} = {}", key, value);
                }
            }
        }
        
        log.info("=== End Metadata ===");
        
        return next.startCall(call, headers);
    }
}
