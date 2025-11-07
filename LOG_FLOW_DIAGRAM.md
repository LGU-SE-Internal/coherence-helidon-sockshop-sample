# Logging Flow with OpenTelemetry Integration

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Helidon Application                         │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    Application Code                          │ │
│  │  - CartResource.java                                         │ │
│  │  - CatalogResource.java                                      │ │
│  │  - etc.                                                      │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                               │                                     │
│                               ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │         JUL Logger (java.util.logging.Logger)                │ │
│  │  - Existing JUL loggers continue to work                     │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                               │                                     │
│                               ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │            JUL-to-SLF4J Bridge (jul-to-slf4j)                │ │
│  │  - Intercepts JUL log calls                                  │ │
│  │  - Forwards to SLF4J                                         │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                               │                                     │
│                               ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                 SLF4J API (slf4j-api)                        │ │
│  │  - Unified logging facade                                    │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                               │                                     │
│                               ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │         Logback (logback-classic + logback.xml)              │ │
│  │  - Logging framework implementation                          │ │
│  │  - Two appenders configured:                                 │ │
│  │    1. Console Appender                                       │ │
│  │    2. OpenTelemetry Appender                                 │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                     │                        │                      │
│                     ▼                        ▼                      │
│  ┌──────────────────────┐    ┌──────────────────────────────────┐ │
│  │  Console Appender    │    │  OpenTelemetry Appender          │ │
│  │  - Formats logs with │    │  - Captures span context         │ │
│  │    trace_id, span_id │    │  - Adds trace_id, span_id to MDC │ │
│  │  - Outputs to console│    │  - Exports to OTel SDK           │ │
│  └──────────────────────┘    └──────────────────────────────────┘ │
│           │                               │                         │
└───────────┼───────────────────────────────┼─────────────────────────┘
            │                               │
            ▼                               ▼
     ┌─────────────┐              ┌─────────────────────────┐
     │   Console   │              │  OpenTelemetry SDK      │
     │   Output    │              │  - Log Signal Pipeline  │
     └─────────────┘              └─────────────────────────┘
                                              │
                                              ▼
                                  ┌─────────────────────────┐
                                  │  OTLP Exporter          │
                                  │  - Sends logs to        │
                                  │    collector/backend    │
                                  └─────────────────────────┘
                                              │
                                              ▼
                                  ┌─────────────────────────┐
                                  │  OpenTelemetry Collector│
                                  │  or Backend             │
                                  │  (e.g., Jaeger, etc.)   │
                                  └─────────────────────────┘
```

## Trace Context Propagation

```
┌─────────────────────────────────────────────────────────────────┐
│                    Incoming HTTP Request                        │
│              (with trace headers: traceparent, etc.)            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  Helidon OpenTelemetry      │
              │  Instrumentation            │
              │  - Extracts trace context   │
              │  - Creates/continues span   │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  Span Context                │
              │  - trace_id: abc123...       │
              │  - span_id: def456...        │
              │  - Made current in thread    │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  Application Code Logs       │
              │  LOGGER.info("Processing")   │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  Logback OpenTelemetry      │
              │  Appender                   │
              │  - Gets current span context│
              │  - Adds to MDC:             │
              │    trace_id = abc123...     │
              │    span_id = def456...      │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  Console Log Output          │
              │  2025.11.05 12:00:00 INFO    │
              │  [trace_id=abc123           │
              │   span_id=def456]           │
              │  Processing request          │
              └──────────────────────────────┘
```

## Before and After Comparison

### Before (JUL)
```
┌──────────────────┐
│  Application     │
│  Code            │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  JUL Logger      │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Console         │
│  - No trace info │
└──────────────────┘

Log Output:
2025.11.05 12:00:00 CONFIG com.example.Service !thread!: Processing
```

### After (SLF4J/Logback with OpenTelemetry)
```
┌──────────────────┐
│  Application     │
│  Code            │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  JUL Logger      │
│  (bridged)       │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  SLF4J/Logback   │
│  + OpenTelemetry │
└────┬────────┬────┘
     │        │
     ▼        ▼
┌─────────┐ ┌──────────────┐
│ Console │ │ OTLP Exporter│
│ + trace │ │ + trace      │
└─────────┘ └──────────────┘

Console Output:
2025.11.05 12:00:00 INFO com.example.Service [http-1] 
[trace_id=abc123 span_id=def456] - Processing

OTLP Export:
{
  "timestamp": "2025-11-05T12:00:00Z",
  "severity": "INFO",
  "body": "Processing",
  "traceId": "abc123...",
  "spanId": "def456...",
  "resource": {
    "service.name": "Carts"
  }
}
```

## Key Integration Points

### 1. Application Startup (Application.java)
```java
public static void main(String... args) {
    // Step 1: Initialize OpenTelemetry
    initializeOpenTelemetry();
    
    // Step 2: Bridge JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    
    // Step 3: Start server
    Server.create().start();
}

private static void initializeOpenTelemetry() {
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
    OpenTelemetryAppender.install(openTelemetry);
}
```

### 2. Logback Configuration (logback.xml)
```xml
<configuration>
    <!-- Console with trace context -->
    <appender name="console" class="...ConsoleAppender">
        <pattern>
            [trace_id=%X{trace_id} span_id=%X{span_id}] - %msg%n
        </pattern>
    </appender>
    
    <!-- OpenTelemetry export -->
    <appender name="OpenTelemetry" 
              class="...OpenTelemetryAppender">
        <captureExperimentalAttributes>true</captureExperimentalAttributes>
    </appender>
    
    <root level="CONFIG">
        <appender-ref ref="console"/>
        <appender-ref ref="OpenTelemetry"/>
    </root>
</configuration>
```

## Benefits of This Architecture

1. **Trace Correlation**: Logs automatically include trace_id and span_id
2. **Unified Export**: Logs go to same backend as traces
3. **Backward Compatible**: Existing JUL loggers continue to work
4. **Standardized**: All services use same logging framework
5. **Flexible**: Can add/remove appenders as needed
6. **Observable**: Complete logs-traces integration

## Dependencies Graph

```
Application Code
    │
    ├─► SLF4J API (2.0.17)
    │       └─► Logback Classic (1.5.20)
    │               ├─► Logback Core (transitive)
    │               └─► OpenTelemetry Logback Appender (2.11.0-alpha)
    │                       └─► OpenTelemetry SDK (from Helidon)
    │
    └─► JUL-to-SLF4J Bridge (2.0.17)
            └─► Routes JUL → SLF4J
```
