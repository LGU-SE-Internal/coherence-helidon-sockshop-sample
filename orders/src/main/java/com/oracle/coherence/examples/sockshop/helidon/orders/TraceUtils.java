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
     * Inject current trace context into a Map.
     * Uses GlobalOpenTelemetry propagator to serialize the context.
     * Falls back to manual W3C traceparent construction if propagator fails.
     *
     * @return Map containing trace headers, or empty map if no active trace
     */
    public static Map<String, String> injectCurrentContext() {
        Map<String, String> carrier = new HashMap<>();
        
        try {
            // Try to use GlobalOpenTelemetry propagator first
            GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), carrier, SETTER);
            
            // If injection succeeded and we have traceparent, return it
            if (carrier.containsKey("traceparent")) {
                return carrier;
            }
        } catch (Exception e) {
            // Propagator may fail due to class loader isolation with Java Agent
            // Fall through to manual construction
        }
        
        // Fallback: manually construct W3C traceparent from current span
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            String traceparent = String.format("00-%s-%s-%02x",
                spanContext.getTraceId(),
                spanContext.getSpanId(),
                spanContext.getTraceFlags().asByte());
            carrier.put("traceparent", traceparent);
        }
        
        return carrier;
    }

    /**
     * Extract trace context from a Map.
     * Uses GlobalOpenTelemetry propagator to deserialize the context.
     *
     * @param carrier Map containing trace headers
     * @return Context with extracted trace information, or root context if extraction fails
     */
    public static Context extractContext(Map<String, String> carrier) {
        if (carrier == null || carrier.isEmpty()) {
            return Context.root();
        }
        
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
     * Check if a context map contains valid trace information.
     *
     * @param carrier Map to check
     * @return true if carrier contains traceparent header
     */
    public static boolean hasTraceContext(Map<String, String> carrier) {
        return carrier != null && carrier.containsKey("traceparent");
    }
}
