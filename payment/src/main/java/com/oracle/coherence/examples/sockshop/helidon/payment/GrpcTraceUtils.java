/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.payment;

import io.grpc.Metadata;
import io.opentelemetry.context.propagation.TextMapGetter;

/**
 * Utility class for gRPC trace context extraction.
 */
public final class GrpcTraceUtils {
    
    private GrpcTraceUtils() {
        // Utility class, no instances
    }
    
    /**
     * TextMapGetter for extracting OpenTelemetry context from gRPC Metadata.
     */
    public static final TextMapGetter<Metadata> METADATA_TEXT_MAP_GETTER = new TextMapGetter<Metadata>() {
        @Override
        public Iterable<String> keys(Metadata carrier) {
            return carrier.keys();
        }
        
        @Override
        public String get(Metadata carrier, String key) {
            return carrier.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
        }
    };
}
