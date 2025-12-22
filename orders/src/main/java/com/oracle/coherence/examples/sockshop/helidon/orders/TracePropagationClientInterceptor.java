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
import io.helidon.tracing.Span;
import io.helidon.tracing.HeaderConsumer;
import io.helidon.tracing.Tracer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Optional;

/**
 * gRPC client interceptor to manually inject trace headers into outgoing gRPC calls.
 * This interceptor extracts the current trace context from Helidon's tracer and
 * injects it as metadata headers for downstream services.
 */
@ApplicationScoped
@Priority(1)
@Slf4j
public class TracePropagationClientInterceptor implements ClientInterceptor {
    
    private static final Metadata.Key<String> TRACEPARENT_KEY = 
            Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Extract traceparent from current span if available
                Optional<Span> currentSpan = Span.current();
                if (currentSpan.isPresent()) {
                    HeaderConsumer consumer = HeaderConsumer.create(new HashMap<>());
                    Tracer.global().inject(currentSpan.get().context(), null, consumer);
                    
                    consumer.get("traceparent").ifPresent(tp -> {
                        headers.put(TRACEPARENT_KEY, tp);
                        log.info(">>>> [CLIENT INTERCEPTOR] Injecting traceparent to gRPC call {}: {}", 
                                method.getFullMethodName(), tp);
                    });
                } else {
                    log.warn(">>>> [CLIENT INTERCEPTOR] No current span available for gRPC call {}", 
                            method.getFullMethodName());
                }
                
                super.start(responseListener, headers);
            }
        };
    }
}
