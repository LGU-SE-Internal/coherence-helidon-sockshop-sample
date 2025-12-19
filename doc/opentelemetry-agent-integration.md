# OpenTelemetry Java Agent Integration Guide

## Overview

This document explains how the Coherence Helidon Sock Shop application integrates with OpenTelemetry, particularly when using the OpenTelemetry Java agent for automatic instrumentation.

## Integration Modes

The application supports two modes of OpenTelemetry integration:

### 1. Agent-Based Instrumentation (Recommended for Production)

When deployed to Kubernetes with the OpenTelemetry Operator, the Java agent is automatically injected via pod annotations:

```yaml
annotations:
  instrumentation.opentelemetry.io/inject-java: "monitoring/opentelemetry-kube-stack"
```

**Behavior with Agent:**
- The agent automatically instruments the application without code changes
- The agent handles all trace creation, span management, and context propagation
- `@WithSpan` annotations are automatically processed by the agent
- The agent sets up `GlobalOpenTelemetry` instance accessible to all libraries
- Logback appender automatically uses the agent's `GlobalOpenTelemetry` for log correlation

**Application Detection:**
Each service's `Application.java` detects the agent presence by checking for agent-specific classes:

```java
private static boolean isAgentPresent() {
    try {
        Class.forName("io.opentelemetry.javaagent.bootstrap.AgentClassLoader");
        return true;
    } catch (ClassNotFoundException e) {
        return false;
    }
}
```

When the agent is detected, the application **skips manual SDK initialization** and relies on the agent.

### 2. Manual SDK Initialization (Development/Testing)

When running locally without the agent, the application initializes its own OpenTelemetry SDK for logs:

**Behavior without Agent:**
- Helidon MP Telemetry creates traces and spans using its built-in support
- The application manually creates an OpenTelemetry SDK instance for log export
- This SDK is installed in the Logback appender for log-to-trace correlation
- `@WithSpan` annotations are processed by Helidon's telemetry support

## How @WithSpan Annotations Work

The `@WithSpan` annotation from `io.opentelemetry.instrumentation.annotations` is used throughout the codebase to create custom spans:

```java
@WithSpan
public void processOrder(Order order) {
    // Business logic
}
```

**With Agent:**
- The agent's `@WithSpan` instrumentation automatically creates spans
- Spans are created in the agent's tracer, which is part of `GlobalOpenTelemetry`
- Span context is automatically propagated through HTTP calls, gRPC, and async operations
- The scope name will be set by the agent based on its configuration

**Without Agent:**
- Helidon MP Telemetry processes `@WithSpan` annotations
- Spans are created using Helidon's built-in OpenTelemetry integration
- The scope name is typically "HELIDON_MICROPROFILE_TELEMETRY"

## Log Correlation with Traces

All services use Logback with two mechanisms for trace correlation:

### 1. OpenTelemetryMdcTurboFilter

A custom TurboFilter that explicitly sets `trace_id` and `span_id` in MDC:

```java
public class OpenTelemetryMdcTurboFilter extends TurboFilter {
    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, 
                             String format, Object[] params, Throwable t) {
        Span span = Span.current();
        SpanContext context = span.getSpanContext();
        if (context.isValid()) {
            MDC.put("trace_id", context.getTraceId());
            MDC.put("span_id", context.getSpanId());
        }
        return FilterReply.NEUTRAL;
    }
}
```

This ensures trace context is available for console logging with patterns like:
```
%d{yyyy.MM.dd HH:mm:ss} %-5level %logger{36} [%thread] traceId=%mdc{trace_id:-} spanId=%mdc{span_id:-} - %msg%n
```

### 2. OpenTelemetryAppender

The Logback appender exports logs to the OTLP collector with automatic trace correlation:

```xml
<appender name="OpenTelemetry"
          class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
    <captureExperimentalAttributes>true</captureExperimentalAttributes>
    <captureKeyValuePairAttributes>true</captureKeyValuePairAttributes>
</appender>
```

**With Agent:**
- The appender automatically uses `GlobalOpenTelemetry` from the agent
- Logs are correlated with traces using the agent's context
- No manual SDK initialization needed

**Without Agent:**
- The application installs a custom SDK in the appender:
  ```java
  OpenTelemetryAppender.install(openTelemetrySdk);
  ```

## Why Only Orders Service Shows Logs Initially

If you notice that only the Orders service shows `spanId` in logs while other services don't, this can happen for several reasons:

1. **Timing of Agent Injection**: The agent might be injected at different times for different pods
2. **Configuration Differences**: Check that all services have the same pod annotations
3. **Logback Initialization**: The TurboFilter might not be configured correctly in all services
4. **Span Context Availability**: Some operations might not have an active span context

**Solution:**
Ensure all services have:
- Pod annotation for agent injection
- `OpenTelemetryMdcTurboFilter` configured in `logback.xml`
- Consistent logging configuration across all services

## Configuration Reference

### Kubernetes Pod Annotation

```yaml
annotations:
  instrumentation.opentelemetry.io/inject-java: "monitoring/opentelemetry-kube-stack"
```

### Application YAML Configuration

```yaml
otel:
  sdk:
    disabled: false  # Helidon SDK for traces
  service:
    name: ServiceName
  traces:
    exporter: otlp
  logs:
    exporter: otlp
  agent:
    present: true  # Informational flag
  exporter:
    otlp:
      traces:
        endpoint: http://opentelemetry-kube-stack-deployment-collector.monitoring:4317
      logs:
        endpoint: http://opentelemetry-kube-stack-deployment-collector.monitoring:4317
```

### Agent Environment Variables

When the agent is injected by the Kubernetes operator, it automatically sets:
- `OTEL_SERVICE_NAME`
- `OTEL_EXPORTER_OTLP_ENDPOINT`
- `OTEL_TRACES_EXPORTER=otlp`
- `OTEL_METRICS_EXPORTER=otlp`
- `OTEL_LOGS_EXPORTER=otlp`

## Troubleshooting

### Check Agent Presence

Look for this message in application logs:
```
OpenTelemetry Java agent detected - using agent's GlobalOpenTelemetry instance
```

Or without agent:
```
No OpenTelemetry agent detected - initializing manual SDK for logs
```

### Verify Trace Context in Logs

Console logs should show:
```
2025.12.19 12:00:00 INFO  com.oracle.coherence... [thread] traceId=abc123... spanId=def456... - Message
```

### Check @WithSpan Behavior

With agent, spans should appear in your trace backend with:
- Proper parent-child relationships
- Span names matching method names
- Attributes from the annotation

### Logback Configuration

Ensure `logback.xml` has both:
1. TurboFilter for MDC population
2. OpenTelemetry appender for log export

## Best Practices

1. **Use the Agent in Production**: It provides zero-code instrumentation and better compatibility
2. **Keep Manual Init for Development**: Allows testing without Kubernetes infrastructure
3. **Consistent Configuration**: Ensure all services have the same Logback and OTel configuration
4. **Monitor Logs**: Check that all services show trace context in their logs
5. **Test Span Propagation**: Verify that traces flow correctly through service boundaries

## References

- [OpenTelemetry Java Agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [Helidon MP Telemetry](https://helidon.io/docs/latest/mp/telemetry)
- [OpenTelemetry Logback Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0)
- [OpenTelemetry Kubernetes Operator](https://opentelemetry.io/docs/kubernetes/operator/)
