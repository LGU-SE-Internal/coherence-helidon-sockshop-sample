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
import io.helidon.tracing.HeaderConsumer;
import io.helidon.tracing.Span;
import io.helidon.tracing.Tracer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * gRPC client interceptor to manually inject trace headers into outgoing gRPC calls.
 * Based on Helidon's GrpcClientTracingInterceptor implementation.
 */
@ApplicationScoped
@Priority(1)
@Slf4j
@Named("traceInterceptor")
@Grpc.GrpcInterceptor
public class TracePropagationClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        log.info(">>>> [CLIENT INTERCEPTOR] interceptCall invoked for method: {}", method.getFullMethodName());
        
        ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {
            
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                log.info(">>>> [CLIENT INTERCEPTOR] start() called, injecting trace headers");
                log.info(">>>> [CLIENT INTERCEPTOR] Before injection, metadata keys: {}", headers.keys());
                
                try {
                    // Get current span and inject trace context into metadata
                    Optional<Span> currentSpan = Span.current();
                    if (currentSpan.isPresent()) {
                        log.info(">>>> [CLIENT INTERCEPTOR] Current span found, injecting context");
                        Tracer.global().inject(currentSpan.get().context(), null, new GrpcHeaderConsumer(headers));
                        log.info(">>>> [CLIENT INTERCEPTOR] After injection, metadata keys: {}", headers.keys());
                    } else {
                        log.warn(">>>> [CLIENT INTERCEPTOR] No current span available");
                    }
                } catch (Exception e) {
                    log.error(">>>> [CLIENT INTERCEPTOR] Error injecting trace context", e);
                }
                
                super.start(responseListener, headers);
            }
        };
    }

    /**
     * HeaderConsumer implementation that writes headers directly to gRPC Metadata.
     * Based on Helidon's GrpcHeaderConsumer.
     */
    private record GrpcHeaderConsumer(Metadata metadata) implements HeaderConsumer {

        @Override
        public void setIfAbsent(String key, String... values) {
            Metadata.Key<String> metadataKey = metadataKey(key);
            if (!metadata.containsKey(metadataKey)) {
                set(metadataKey, values);
            }
        }

        @Override
        public void set(String key, String... values) {
            set(metadataKey(key), values);
        }

        @Override
        public Iterable<String> keys() {
            return metadata.keys();
        }

        @Override
        public Optional<String> get(String key) {
            Metadata.Key<String> metadataKey = metadataKey(key);
            return Optional.ofNullable(metadata.get(metadataKey));
        }

        @Override
        public Iterable<String> getAll(String key) {
            Metadata.Key<String> metadataKey = metadataKey(key);
            return metadata.containsKey(metadataKey) ? metadata.getAll(metadataKey) : List.of();
        }

        @Override
        public boolean contains(String key) {
            return metadata.containsKey(metadataKey(key));
        }

        private Metadata.Key<String> metadataKey(String key) {
            return Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
        }

        private void set(Metadata.Key<String> key, String... values) {
            for (String value : values) {
                metadata.put(key, value);
            }
        }
    }
}

