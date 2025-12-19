# Root Cause Analysis: Helidon SDK vs Java Agent Conflict

## The Problem

Only the Orders service had logs that correlated with traces. All other services (carts, catalog, payment, shipping, users) did not correlate logs with traces, even though:
- All services had identical code
- All services had identical logback.xml configuration
- All services had the OpenTelemetry Java agent injected
- All services had the post-startup appender installation

## Investigation

The user reported that the correlation in Orders was happening with:
> `EventDrivenOrderProcessor.saveOrder io.opentelemetry.opentelemetry-instrumentation-annotations-1.16`

This indicated that:
1. The Java agent WAS working (it instruments `@WithSpan` annotations)
2. Logs inside `@WithSpan` methods WERE being correlated
3. Logs in HTTP endpoint methods were NOT being correlated

## Root Cause Discovered

### The Configuration Issue

All services had in `application.yaml`:
```yaml
otel:
  sdk:
    disabled: false  # ← THIS IS THE PROBLEM!
```

When `otel.sdk.disabled: false`, **Helidon MP Telemetry creates its own OpenTelemetry SDK** to instrument HTTP requests and other Helidon features.

### The Conflict

With the Java agent present AND Helidon SDK enabled:

1. **Helidon MP Telemetry SDK**:
   - Created by Helidon at application startup
   - Instruments HTTP requests (JAX-RS endpoints)
   - Creates spans for incoming HTTP calls
   - Has its own span storage and context

2. **OpenTelemetry Java Agent**:
   - Injected via Kubernetes annotation
   - Creates GlobalOpenTelemetry
   - Instruments `@WithSpan` annotations
   - Instruments gRPC, JDBC, etc.
   - Has its own span storage and context

3. **Our Logback Appender**:
   - Installed with the agent's GlobalOpenTelemetry
   - Can only correlate logs with spans in the agent's SDK
   - Cannot see spans in Helidon's separate SDK

### Why Only Orders Worked

**Orders Service Logs:**
```java
@WithSpan  // ← Agent creates this span
protected void saveOrder(Order order) {
    orders.saveOrder(order);
    log.info("Order saved: " + order);  // ← Inside agent's span ✓
}
```

**Other Services Logs:**
```java
@GET  // ← Helidon SDK creates span for this HTTP request
@Path("{customerId}")
public Cart getCart(String customerId) {
    LOGGER.info("Getting cart for customer: " + customerId);  // ← Inside Helidon's span ✗
    return carts.getOrCreateCart(customerId);
}
```

The logging in Orders happened inside `@WithSpan` methods (agent's spans), while logging in other services happened in HTTP endpoint methods (Helidon's spans). Since the logback appender was installed with the agent's SDK, it could only correlate with agent's spans!

## The Fix

### Solution: Disable Helidon SDK

Changed `application.yaml` in all services:

```yaml
otel:
  sdk:
    disabled: true  # When Java agent is present, let it handle everything
  service:
    name: ServiceName
```

### Why This Works

With `otel.sdk.disabled: true`:
1. **Helidon will NOT create its own SDK**
2. **Helidon will use GlobalOpenTelemetry from the agent**
3. **Agent instruments EVERYTHING**:
   - HTTP requests (via JAX-RS instrumentation)
   - @WithSpan annotations
   - gRPC calls
   - JDBC queries
   - etc.
4. **Single unified span context**
5. **Logback appender correlates with ALL spans**

## Expected Behavior After Fix

### All Services Should Now:

1. **HTTP Endpoint Logs Correlate**
   ```
   GET /carts/{id}
   ↓
   Agent creates span for HTTP request
   ↓
   LOGGER.info("Getting cart...") ← Correlates with HTTP span ✓
   ```

2. **@WithSpan Method Logs Correlate**
   ```
   @WithSpan method call
   ↓
   Agent creates span
   ↓
   log.info("Processing...") ← Correlates with method span ✓
   ```

3. **Nested Spans Work Correctly**
   ```
   HTTP Request Span (agent)
   └─ @WithSpan Method Span (agent)
      └─ gRPC Call Span (agent)
   ```

4. **All Logs Exported**
   - Console: `traceId=abc spanId=def`
   - OTLP: Logs with trace correlation
   - Observability Platform: Full correlation

## Technical Details

### Helidon MP Telemetry Behavior

From Helidon documentation:
- When `otel.sdk.disabled=false`: Helidon creates and configures its own OpenTelemetry SDK
- When `otel.sdk.disabled=true`: Helidon uses GlobalOpenTelemetry if available
- When Java agent is present: GlobalOpenTelemetry is set by the agent

### Configuration Priority

With agent present:
```
otel.sdk.disabled=true
    ↓
Helidon checks GlobalOpenTelemetry
    ↓
Agent has already set GlobalOpenTelemetry
    ↓
Helidon uses agent's instance ✓
```

With agent present but otel.sdk.disabled=false:
```
otel.sdk.disabled=false
    ↓
Helidon creates its own SDK
    ↓
Agent also sets GlobalOpenTelemetry
    ↓
Two separate SDKs running ✗
```

## Verification Steps

After deploying the fix:

1. **Check All Services Start Successfully**
   ```bash
   kubectl get pods -n sockshop
   ```

2. **Verify Agent Detection**
   ```bash
   kubectl logs <pod-name> | grep "OpenTelemetry"
   # Should see: "OpenTelemetry Java agent detected..."
   # Should see: "Successfully installed agent's GlobalOpenTelemetry..."
   ```

3. **Test HTTP Endpoints**
   ```bash
   curl http://carts-service/carts/test123
   ```
   Check logs - should show traceId and spanId

4. **Test Each Service**
   - Carts: GET /carts/{id}
   - Catalog: GET /catalogue
   - Payment: POST /payments
   - Shipping: POST /shipments
   - Users: GET /customers/{id}
   - Orders: POST /orders

5. **Verify in Observability Platform**
   - Open any trace
   - Should see HTTP request spans
   - Should see method spans from @WithSpan
   - Should see logs attached to ALL spans
   - All services should show up

## Key Lessons

1. **Agent vs SDK**: When using OpenTelemetry Java agent, disable framework SDKs
2. **GlobalOpenTelemetry**: Only one instance should exist per application
3. **Helidon Configuration**: `otel.sdk.disabled=true` with agent
4. **Span Context**: Logs only correlate with spans in the same SDK instance
5. **Debugging**: Look for multiple SDK instances when correlation fails

## Related Configuration

### What Still Needs `otel.sdk.disabled=false`

Only when running **WITHOUT** the Java agent (e.g., local development):
- Helidon needs to create its own SDK for tracing
- Our manual log SDK initialization works
- Single Helidon SDK handles everything

### What Needs `otel.sdk.disabled=true`

When running **WITH** the Java agent (e.g., Kubernetes):
- Agent handles all instrumentation
- Helidon uses agent's GlobalOpenTelemetry
- Single agent SDK handles everything

## Files Changed

Commit `d702fe6`:
- `carts/src/main/resources/application.yaml` - `disabled: false` → `true`
- `catalog/src/main/resources/application.yaml` - `disabled: false` → `true`
- `orders/src/main/resources/application.yaml` - `disabled: false` → `true`
- `payment/src/main/resources/application.yaml` - `disabled: false` → `true`
- `shipping/src/main/resources/application.yaml` - `disabled: false` → `true`
- `users/src/main/resources/application.yaml` - `disabled: false` → `true`

## Summary

The issue was NOT with our code or timing - it was a fundamental configuration conflict. Helidon was creating its own OpenTelemetry SDK while the Java agent was also creating one, causing spans to be split across two separate SDK instances. Disabling Helidon's SDK (`otel.sdk.disabled: true`) ensures the agent is the single source of truth for all OpenTelemetry instrumentation.
