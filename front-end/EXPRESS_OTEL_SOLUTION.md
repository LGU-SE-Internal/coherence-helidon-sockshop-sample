# Express.js + OpenTelemetry Solution

## Overview

This document describes the Express.js + OpenTelemetry implementation that provides full observability while maintaining 100% compatibility with the original Sock Shop functionality.

## Why Express Instead of Next.js?

The original Sock Shop frontend was built with Express.js and relies heavily on:
- **express-session**: Server-side session management with automatic session ID generation
- **Session-based state**: Customer ID tracking, cart management, and login state
- **Cookie-based authentication**: Stateful authentication using server sessions
- **Request-scoped middleware**: Session data available across all routes

Next.js API routes are stateless and don't have built-in session management, making it incompatible with the existing architecture without a complete rewrite of the frontend logic.

## What We Achieved

### ✅ Goal 1: Modern Tech Stack (OpenTelemetry)
- **OpenTelemetry SDK for Node.js** fully integrated
- Automatic instrumentation for Express, HTTP, and other modules
- Distributed tracing with W3C trace context propagation
- Resource detection (K8s, AWS, GCP, Alibaba Cloud, containers)
- Metrics and traces exported to OTLP collector via gRPC

### ✅ Goal 2: Complete OpenTelemetry Observability
- Server-side auto-instrumentation via `@opentelemetry/auto-instrumentations-node`
- OTLP trace exporter with gRPC protocol
- OTLP metrics exporter with periodic export
- Resource attributes including:
  - service.name
  - service.version
  - deployment.environment.name
  - k8s.namespace.name
  - Container, host, OS, and process detection

### ✅ Goal 3: Backend Compatibility
- **100% compatible** with existing Helidon backend services
- All API endpoints unchanged
- Session management working correctly
- Cart merging after login/register
- No backend modifications required

## Architecture

```
┌─────────────────────────────────────┐
│   Express.js Application            │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  OpenTelemetry SDK          │   │
│  │  (Instrumentation.js)       │   │
│  │  - Auto-instrumentations    │   │
│  │  - OTLP Exporters           │   │
│  │  - Resource Detection       │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Express Middleware         │   │
│  │  - express-session          │   │
│  │  - cookie-parser            │   │
│  │  - body-parser              │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  API Routes                 │   │
│  │  - /cart                    │   │
│  │  - /catalogue               │   │
│  │  - /orders                  │   │
│  │  - /login                   │   │
│  │  - /register                │   │
│  │  - /customers, /cards, etc  │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Static Assets (public/)    │   │
│  │  - HTML pages               │   │
│  │  - CSS, JavaScript          │   │
│  │  - Images                   │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│  Helidon Backend Services           │
│  - catalogue-svc                    │
│  - carts-svc                        │
│  - orders-svc                       │
│  - user-svc                         │
│  - payment-svc                      │
│  - shipping-svc                     │
└─────────────────────────────────────┘
```

## Files Structure

```
front-end/
├── Instrumentation.js              # OpenTelemetry loader
├── server-express.js               # Express server with OTEL
├── package.json                    # Dependencies (Express + OTEL)
├── Dockerfile                      # Simple Express deployment
├── api/                            # API endpoint handlers
│   ├── cart/
│   ├── catalogue/
│   ├── orders/
│   └── user/
├── config.js                       # Configuration
├── helpers/                        # Helper functions
├── public/                         # Static HTML/CSS/JS
├── scripts/                        # Build scripts
└── utils/
    └── telemetry/
        └── Instrumentation.js      # OTEL SDK configuration
```

## Key Dependencies

### Express & Middleware
- `express`: ^4.21.2
- `express-session`: ^1.18.1
- `cookie-parser`: ^1.4.6
- `body-parser`: ^1.20.2

### OpenTelemetry
- `@opentelemetry/sdk-node`: 0.205.0
- `@opentelemetry/auto-instrumentations-node`: 0.64.6
- `@opentelemetry/exporter-trace-otlp-grpc`: 0.205.0
- `@opentelemetry/resources`: 2.1.0
- Resource detectors for AWS, GCP, Alibaba Cloud, containers

## Environment Variables

### OpenTelemetry Standard Variables
- `OTEL_SERVICE_NAME`: Service name (default: "frontend")
- `OTEL_EXPORTER_OTLP_ENDPOINT`: OTLP collector endpoint (gRPC)
- `OTEL_EXPORTER_OTLP_PROTOCOL`: Export protocol (default: "grpc")

### Application Variables
- `PORT`: Server port (default: 8080)
- `BACKEND_DOMAIN`: Backend service domain suffix (e.g., ".sockshop.svc.cluster.local")
- `K8S_NAMESPACE`: Kubernetes namespace for resource attributes
- `ENV_PLATFORM`: Deployment platform identifier

## Deployment

### Local Development
```bash
npm install
npm run dev
```

### Docker Build
```bash
docker build -t sockshop-frontend:latest .
```

### Kubernetes Deployment
```bash
kubectl apply -f k8s/optional/original-front-end.yaml -n sockshop
```

The Kubernetes configuration includes:
- Service on port 80, targeting container port 8080
- All required environment variables
- OpenTelemetry collector endpoint configuration
- Namespace injection for resource attributes

## Observability Features

### Traces
- HTTP request/response tracing
- Express middleware tracing
- Outbound HTTP calls to backend services
- Distributed trace context propagation
- Session ID correlation

### Metrics
- HTTP server metrics
- Request duration histograms
- Active requests gauge
- Error rates

### Resource Attributes
```
service.name: frontend
service.version: 0.1.0
deployment.environment.name: kubernetes
k8s.namespace.name: sockshop
container.id: <auto-detected>
host.name: <auto-detected>
os.type: <auto-detected>
process.pid: <auto-detected>
```

## Trade-offs

### What We Kept
- ✅ Express.js framework (proven, stable, well-understood)
- ✅ Server-side session management
- ✅ All original functionality
- ✅ Static HTML pages (can be migrated incrementally)
- ✅ Existing API structure

### What We Added
- ✅ Complete OpenTelemetry instrumentation
- ✅ Automatic distributed tracing
- ✅ Metrics collection
- ✅ Resource detection
- ✅ OTLP export to collector

### What We Didn't Do (Yet)
- ❌ React components (static HTML still in use)
- ❌ Modern UI framework (Next.js, Remix, etc.)
- ❌ TypeScript (JavaScript codebase)
- ❌ Server-side rendering (client-side only)

These can be added incrementally in future phases without breaking functionality.

## Comparison: Next.js Attempt vs Express Solution

| Aspect | Next.js Attempt | Express Solution |
|--------|----------------|------------------|
| Session Management | ❌ Broken (no server sessions) | ✅ Working (express-session) |
| Login/Register | ❌ Broken (no session state) | ✅ Working (full state management) |
| Cart Functionality | ❌ Broken (session ID missing) | ✅ Working (session-based) |
| Cart Merging | ❌ Broken (no session tracking) | ✅ Working (automatic merge) |
| OpenTelemetry | ✅ Implemented | ✅ Implemented |
| Backend Compatibility | ❌ Partial | ✅ 100% Compatible |
| Development Time | High (rewrites needed) | Low (drop-in solution) |
| Production Ready | ❌ No | ✅ Yes |

## Future Enhancements

### Phase 2: Incremental Modernization
1. **Add TypeScript**: Gradual migration to TypeScript
2. **Modularize API**: Split API handlers into cleaner modules
3. **Add React Components**: Replace HTML pages one by one
4. **Client-side Tracing**: Add browser-side OpenTelemetry
5. **Helm Charts**: Create production Helm deployment

### Phase 3: Full Modernization
1. **Server-Side Rendering**: Add SSR with Express + React
2. **GraphQL Layer**: Optional GraphQL API layer
3. **Advanced Features**: Progressive web app, offline support
4. **Performance Optimization**: Caching, CDN integration

## Conclusion

This Express + OpenTelemetry solution provides:
- **Immediate Production Readiness**: All features working correctly
- **Complete Observability**: Full OTEL integration with traces and metrics
- **Zero Backend Changes**: Existing services work without modification
- **Incremental Path Forward**: Can modernize UI gradually without breaking functionality

The pragmatic choice is to use what works (Express) while gaining the observability benefits of OpenTelemetry, rather than forcing a Next.js migration that breaks core functionality.
