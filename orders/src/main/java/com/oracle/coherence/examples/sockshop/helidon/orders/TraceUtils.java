/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.orders;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for trace context propagation across async boundaries.
 * Handles serialization and deserialization of OpenTelemetry context using W3C traceparent format.
 */
public class TraceUtils {
    
    private static final TextMapSetter<Map<String, String>> SETTER = Map::put;
    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

    /**
     * Inject current trace context as W3C traceparent string.
     * Manually constructs traceparent from current span to avoid class loader issues with Java Agent.
     *
     * @return W3C traceparent string, or null if no active trace
     */
    public static String injectCurrentTraceParent() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            return String.format("00-%s-%s-%02x",
                spanContext.getTraceId(),
                spanContext.getSpanId(),
                spanContext.getTraceFlags().asByte());
        }
        return null;
    }

    /**
     * Extract trace context from W3C traceparent string.
     * Uses GlobalOpenTelemetry propagator to deserialize the context.
     *
     * @param traceParent W3C traceparent string
     * @return Context with extracted trace information, or root context if extraction fails
     */
    public static Context extractContext(String traceParent) {
        if (traceParent == null || traceParent.isEmpty()) {
            return Context.root();
        }
        
        // Convert string to Map for propagator
        Map<String, String> carrier = new HashMap<>();
        carrier.put("traceparent", traceParent);
        
        try {
            return GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.root(), carrier, GETTER);
        } catch (Exception e) {
            // If extraction fails, return root context
            return Context.root();
        }
    }

    /**
     * Check if traceparent string is valid.
     *
     * @param traceParent traceparent string to check
     * @return true if traceparent is not null and not empty
     */
    public static boolean hasTraceContext(String traceParent) {
        return traceParent != null && !traceParent.isEmpty();
    }
}
