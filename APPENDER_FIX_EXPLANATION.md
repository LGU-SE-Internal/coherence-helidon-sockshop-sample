# Fix for Missing Log Export When OpenTelemetry Agent is Present

## Problem Identified

After the initial fix was deployed, logs were still not being exported to OTLP even though the OpenTelemetry Java agent was present. The issue was that while we correctly detected the agent and skipped manual SDK initialization, **we forgot to install the agent's GlobalOpenTelemetry instance in the logback appender**.

## Root Cause

The `OpenTelemetryAppender` in logback needs to be explicitly told which `OpenTelemetry` instance to use for exporting logs. In our previous fix:

1. ✅ We detected the agent correctly
2. ✅ We skipped manual SDK initialization 
3. ❌ **But we didn't install the agent's GlobalOpenTelemetry in the appender**
4. ❌ Result: The appender had no OpenTelemetry instance → logs weren't exported

## The Fix

We now explicitly install the agent's GlobalOpenTelemetry instance when the agent is detected:

```java
if (isAgentPresent()) {
    System.out.println("OpenTelemetry Java agent detected - using agent's GlobalOpenTelemetry instance");
    try {
        // Get the GlobalOpenTelemetry instance from the agent
        OpenTelemetry globalOtel = io.opentelemetry.api.GlobalOpenTelemetry.get();
        
        // Install it in the logback appender so logs are correlated with traces
        io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender.install(globalOtel);
        
        System.out.println("Installed agent's GlobalOpenTelemetry in logback appender");
    } catch (Exception e) {
        System.err.println("Failed to install agent's GlobalOpenTelemetry in logback appender: " + e.getMessage());
    }
    return;
}
```

## What Changed

**Before (broken):**
```java
if (isAgentPresent()) {
    System.out.println("Agent detected");
    return; // ← Left immediately without installing appender!
}
```

**After (fixed):**
```java
if (isAgentPresent()) {
    System.out.println("Agent detected");
    OpenTelemetry globalOtel = GlobalOpenTelemetry.get();
    OpenTelemetryAppender.install(globalOtel); // ← Now properly installed!
    return;
}
```

## Expected Behavior After This Fix

### In Application Logs
You should see these two messages when the application starts:
```
OpenTelemetry Java agent detected - using agent's GlobalOpenTelemetry instance
Installed agent's GlobalOpenTelemetry in logback appender
```

### In Console Output
All service logs should show trace context:
```
2025.12.19 13:00:00 INFO  com.oracle.coherence... [thread] traceId=abc123def456 spanId=789ghi012 - Message here
```

### In Observability Platform
1. ✅ Traces appear with proper span hierarchy
2. ✅ Logs are exported to OTLP collector
3. ✅ Logs are correlated with traces (same traceId)
4. ✅ All services (carts, catalog, orders, payment, shipping, users) show logs

## How to Verify

1. **Deploy the updated services** (commit 596fdf6 or later)

2. **Check startup logs** for both messages:
   ```bash
   kubectl logs <pod-name> | grep "OpenTelemetry"
   ```

3. **Check running service logs** for trace context:
   ```bash
   kubectl logs <pod-name> | grep "traceId="
   ```

4. **In your observability platform** (e.g., Grafana, Jaeger):
   - View a trace
   - Check if logs are attached to the trace
   - Verify all services show logs

## Technical Details

### Why GlobalOpenTelemetry.get() Works

When the OpenTelemetry Java agent is attached:
1. The agent automatically initializes a `GlobalOpenTelemetry` instance
2. This instance is configured with OTLP exporters for traces, metrics, and logs
3. Calling `GlobalOpenTelemetry.get()` returns this agent-managed instance
4. Installing it in the appender connects logback to the agent's log export pipeline

### Error Handling

The fix includes error handling in case:
- GlobalOpenTelemetry isn't initialized yet
- The appender installation fails
- Any other unexpected errors occur

Errors are logged to stderr but don't crash the application.

## Testing Results

- ✅ All 6 services compile successfully
- ✅ CodeQL security scan: 0 alerts
- ✅ No new dependencies added
- ✅ Backward compatible (still works without agent)

## Commits

- `596fdf6`: Fix: Install agent's GlobalOpenTelemetry in logback appender when agent is present
- `1ddf95d`: Update Chinese documentation with explanation of the appender installation fix

## Summary

This was a critical bug in the initial fix. The detection logic was correct, but we didn't complete the wiring needed to make the logback appender work with the agent. Now with the GlobalOpenTelemetry installation in place, logs should be properly exported to OTLP and correlated with traces across all services.
