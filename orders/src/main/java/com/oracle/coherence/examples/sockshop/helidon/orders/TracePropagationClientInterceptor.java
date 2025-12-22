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
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.stub.MetadataUtils;
import io.helidon.tracing.Span;
import io.helidon.tracing.HeaderConsumer;
import io.helidon.tracing.Tracer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * gRPC client interceptor to manually inject trace headers into outgoing gRPC calls.
 * Uses MetadataUtils pattern to attach headers to requests.
 */
@ApplicationScoped
@Priority(1)
@Slf4j
public class TracePropagationClientInterceptor implements ClientInterceptor {
    
    private static final Metadata.Key<String> TRACEPARENT_KEY = 
            Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER);
    
    private static final java.util.Map<String, java.util.List<String>> EMPTY_MAP = java.util.Collections.emptyMap();
    
    private final ClientInterceptor attachHeadersInterceptor;
    
    public TracePropagationClientInterceptor() {
        // Create metadata with trace headers
        Metadata metadata = createMetadataWithTraceHeaders();
        // Use MetadataUtils to create the interceptor
        this.attachHeadersInterceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
    }
    
    private Metadata createMetadataWithTraceHeaders() {
        Metadata metadata = new Metadata();
        
        // Extract traceparent from current span if available
        Optional<Span> currentSpan = Span.current();
        if (currentSpan.isPresent()) {
            HeaderConsumer consumer = HeaderConsumer.create(EMPTY_MAP);
            Tracer.global().inject(currentSpan.get().context(), null, consumer);
            
            consumer.get("traceparent").ifPresent(tp -> {
                metadata.put(TRACEPARENT_KEY, tp);
                log.debug(">>>> [CLIENT INTERCEPTOR] Creating metadata with traceparent: {}", tp);
            });
        } else {
            log.debug(">>>> [CLIENT INTERCEPTOR] No current span available");
        }
        
        return metadata;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        // Recreate metadata for each call to get current trace context
        Metadata metadata = createMetadataWithTraceHeaders();
        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
        
        return interceptor.interceptCall(method, callOptions, next);
    }
}

