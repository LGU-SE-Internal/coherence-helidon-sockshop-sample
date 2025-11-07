# OpenTelemetry Log Correlation Integration

## Overview

This document describes the integration of OpenTelemetry log appenders to enable trace correlation in logs across all microservices in the Helidon Sockshop application.

## Problem Statement

The original requirement (translated from Chinese) was:
"We need to add trace correlation to logs based on the ./opentelemetry-java-examples-log-appender reference. We need to:
1. Investigate the current log SDK used by services
2. For SDKs that are compatible with the examples, adapt them directly; for incompatible SDKs, migrate to compatible ones"

## Investigation Results

### Current Logging Setup (Before)
- **Logging Framework**: Java Util Logging (JUL) via `helidon-logging-jul`
- **Configuration**: `logging.properties` in each service
- **Handler**: `io.helidon.logging.jul.HelidonConsoleHandler`
- **Issue**: No native OpenTelemetry JUL appender available

### Reference Example Analysis
The `opentelemetry-java-examples-log-appender` directory shows:
- OpenTelemetry supports Log4j2 and Logback (SLF4J) appenders
- JUL logs can be bridged to SLF4J using `jul-to-slf4j`
- Trace context (trace_id, span_id) is automatically propagated to logs when recorded within a span

## Solution

### Migration Strategy
Since OpenTelemetry doesn't provide a native JUL handler, we migrated from JUL to SLF4J/Logback:

1. **Replaced JUL with SLF4J/Logback**
   - Added `slf4j-api` and `logback-classic` dependencies
   - Added `jul-to-slf4j` bridge for JUL compatibility
   
2. **Added OpenTelemetry Log Appenders**
   - Added `opentelemetry-logback-appender-1.0` dependency
   - Added OpenTelemetry SDK dependencies for log processing

3. **Updated Application Initialization**
   - Initialize OpenTelemetry before starting the server
   - Install the OpenTelemetry appender using the global OpenTelemetry instance
   - Bridge JUL logs to SLF4J for backward compatibility

4. **Created Logback Configuration**
   - Console appender with trace context in log pattern
   - OpenTelemetry appender for exporting logs with trace correlation

## Changes Made

### Services Updated
All microservices have been updated with the same changes:
- ✅ carts
- ✅ catalog
- ✅ orders
- ✅ payment
- ✅ shipping
- ✅ users

### Files Modified Per Service

#### 1. `pom.xml`
**Removed:**
```xml
<dependency>
    <groupId>io.helidon.logging</groupId>
    <artifactId>helidon-logging-jul</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Added:**
```xml
<!-- SLF4J / Logback for logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.17</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.20</version>
</dependency>
<!-- JUL to SLF4J bridge -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jul-to-slf4j</artifactId>
    <version>2.0.17</version>
</dependency>
<!-- OpenTelemetry log appender for Logback -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-logback-appender-1.0</artifactId>
    <version>2.11.0-alpha</version>
</dependency>
<!-- OpenTelemetry SDK for logs -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry.semconv</groupId>
    <artifactId>opentelemetry-semconv</artifactId>
    <version>1.28.0-alpha</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk-logs</artifactId>
</dependency>
```

#### 2. `Application.java`
Updated the main application class to initialize OpenTelemetry and bridge JUL:

```java
public static void main(String... args) {
    // Initialize OpenTelemetry before starting the server
    initializeOpenTelemetry();
    
    // Route JUL logs to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    
    Server.create().start();
}

private static void initializeOpenTelemetry() {
    // Get the global OpenTelemetry instance configured by Helidon
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
    
    // Install OpenTelemetry in the Logback appender
    io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender.install(
        openTelemetry);
}
```

#### 3. `logback.xml` (New File)
Created new Logback configuration file in `src/main/resources/`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy.MM.dd HH:mm:ss} %-5level %logger{36} [%thread] [trace_id=%X{trace_id} span_id=%X{span_id}] - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="OpenTelemetry"
              class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
        <captureExperimentalAttributes>true</captureExperimentalAttributes>
        <captureKeyValuePairAttributes>true</captureKeyValuePairAttributes>
    </appender>
    
    <root level="CONFIG">
        <appender-ref ref="console"/>
        <appender-ref ref="OpenTelemetry"/>
    </root>
</configuration>
```

#### 4. `logging.properties`
- Renamed to `logging.properties.bak` for backup
- No longer used (replaced by `logback.xml`)

## How It Works

### Trace Correlation
When a request comes in with trace context:
1. Helidon's OpenTelemetry integration creates a span
2. The span context (trace_id, span_id) is made current
3. When logging occurs, Logback's OpenTelemetry appender:
   - Captures the current span context
   - Adds trace_id and span_id to the log MDC (Mapped Diagnostic Context)
   - Includes trace context in the console output
   - Exports the log record with trace correlation to the OpenTelemetry collector

### Log Output Format
Logs now include trace correlation information:
```
2025.11.05 05:27:00 INFO  com.oracle.coherence.examples.sockshop.helidon.carts.CartResource [http-thread-1] [trace_id=abc123 span_id=def456] - Processing cart request
```

### JUL Compatibility
The `jul-to-slf4j` bridge ensures that any existing JUL loggers in the codebase continue to work:
- JUL log calls are intercepted
- Forwarded to SLF4J
- Processed by Logback appenders
- Trace context is preserved

## Testing

All unit tests pass successfully after the migration:
```
[INFO] Reactor Summary for sockshop-coh 2.3.3:
[INFO] 
[INFO] carts .............................................. SUCCESS
[INFO] catalog ............................................ SUCCESS
[INFO] orders ............................................. SUCCESS
[INFO] payment ............................................ SUCCESS
[INFO] shipping ........................................... SUCCESS
[INFO] users .............................................. SUCCESS
```

## Benefits

1. **Trace Correlation**: Logs are now correlated with traces, making debugging distributed systems easier
2. **Standardization**: All services use the same logging framework (SLF4J/Logback)
3. **Backward Compatibility**: JUL loggers still work through the bridge
4. **OpenTelemetry Integration**: Logs can be exported to any OpenTelemetry collector
5. **Enhanced Observability**: Logs, traces, and metrics are now integrated

## Configuration

### Log Level
The default log level is set to `CONFIG` in `logback.xml`. To change it:
```xml
<root level="INFO">  <!-- Change CONFIG to INFO, DEBUG, etc. -->
    <appender-ref ref="console"/>
    <appender-ref ref="OpenTelemetry"/>
</root>
```

### Trace Context Pattern
The console pattern includes trace context. To customize:
```xml
<pattern>%d{yyyy.MM.dd HH:mm:ss} %-5level %logger{36} [%thread] [trace_id=%X{trace_id} span_id=%X{span_id}] - %msg%n</pattern>
```

### OpenTelemetry Exporter
The services use Helidon's OpenTelemetry configuration. Configure the OTLP endpoint using environment variables or `microprofile-config.properties`:
```properties
otel.exporter.otlp.endpoint=http://localhost:4317
otel.service.name=YourServiceName
```

## References

- [OpenTelemetry Logback Appender](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0)
- [SLF4J JUL Bridge](https://www.slf4j.org/legacy.html#jul-to-slf4j)
- [Helidon Microprofile Telemetry](https://helidon.io/docs/v4/mp/telemetry)
- Reference Example: `./opentelemetry-java-examples-log-appender`
