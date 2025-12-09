/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.examples.sockshop.helidon.orders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;
import org.slf4j.Marker;

/**
 * A TurboFilter that explicitly sets the trace ID and span ID in the MDC
 * from the current OpenTelemetry span context.
 * 
 * This is needed because the MDC instrumentation in OpenTelemetry doesn't actually
 * change the MDC content - it just intercepts calls to ILoggingEvent#getMDCPropertyMap.
 * By explicitly setting the MDC values, we ensure they are cached and available
 * for deferred processing (e.g., async appenders).
 * 
 * @see <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/13069#issuecomment-3612553350">GitHub Issue</a>
 */
public class OpenTelemetryMdcTurboFilter extends TurboFilter {

    private static final String TRACE_ID = "trace_id";
    private static final String SPAN_ID = "span_id";

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        Span span = Span.current();
        SpanContext context = span.getSpanContext();
        if (context.isValid()) {
            MDC.put(TRACE_ID, context.getTraceId());
            MDC.put(SPAN_ID, context.getSpanId());
        } else {
            // Clear stale trace context when span is invalid
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
        }
        return FilterReply.NEUTRAL;
    }
}
