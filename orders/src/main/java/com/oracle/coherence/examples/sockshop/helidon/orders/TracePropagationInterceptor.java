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

import java.util.Map;

/**
 * gRPC ClientInterceptor that manually injects trace propagation headers.
 * Uses ThreadLocal to store headers that should be propagated with the next gRPC call.
 */
public class TracePropagationInterceptor implements ClientInterceptor {
    
    private static final ThreadLocal<Map<String, String>> TRACE_HEADERS = new ThreadLocal<>();
    
    /**
     * Set trace headers for the current thread.
     * These headers will be attached to the next gRPC call made from this thread.
     *
     * @param headers map of header names to values
     */
    public static void setTraceHeaders(Map<String, String> headers) {
        TRACE_HEADERS.set(headers);
    }
    
    /**
     * Clear trace headers for the current thread.
     */
    public static void clearTraceHeaders() {
        TRACE_HEADERS.remove();
    }
    
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Inject trace headers from ThreadLocal
                Map<String, String> traceHeaders = TRACE_HEADERS.get();
                if (traceHeaders != null) {
                    traceHeaders.forEach((key, value) -> {
                        Metadata.Key<String> metadataKey = 
                            Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                        headers.put(metadataKey, value);
                    });
                }
                
                super.start(responseListener, headers);
            }
        };
    }
}
