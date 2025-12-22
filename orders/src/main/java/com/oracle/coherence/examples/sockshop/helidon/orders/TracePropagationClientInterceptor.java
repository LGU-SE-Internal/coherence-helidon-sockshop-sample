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
    
    private Metadata createMetadataWithTraceHeaders() {
        Metadata metadata = new Metadata();
        
        log.info(">>>> [CLIENT INTERCEPTOR] Creating metadata - checking for current span");
        
        // Extract traceparent from current span if available
        Optional<Span> currentSpan = Span.current();
        if (currentSpan.isPresent()) {
            log.info(">>>> [CLIENT INTERCEPTOR] Current span found, extracting trace context");
            HeaderConsumer consumer = HeaderConsumer.create(EMPTY_MAP);
            Tracer.global().inject(currentSpan.get().context(), null, consumer);
            
            Optional<String> traceparent = consumer.get("traceparent");
            if (traceparent.isPresent()) {
                String tp = traceparent.get();
                metadata.put(TRACEPARENT_KEY, tp);
                log.info(">>>> [CLIENT INTERCEPTOR] Injecting traceparent to metadata: {}", tp);
            } else {
                log.warn(">>>> [CLIENT INTERCEPTOR] No traceparent found in HeaderConsumer");
            }
        } else {
            log.warn(">>>> [CLIENT INTERCEPTOR] No current span available for trace propagation");
        }
        
        return metadata;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        log.info(">>>> [CLIENT INTERCEPTOR] interceptCall invoked for method: {}", method.getFullMethodName());
        
        // Recreate metadata for each call to get current trace context
        Metadata metadata = createMetadataWithTraceHeaders();
        
        log.info(">>>> [CLIENT INTERCEPTOR] Metadata keys: {}", metadata.keys());
        
        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
        
        return interceptor.interceptCall(method, callOptions, next);
    }
}

