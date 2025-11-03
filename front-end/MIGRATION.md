# Front-End Refactoring Migration Guide

## Overview

The Sock Shop front-end has been refactored from a legacy Express.js application to a modern Next.js application with full OpenTelemetry observability support.

## What Changed

### Technology Stack Upgrade

**Before:**
- Node.js 10
- Express.js
- No TypeScript
- No OpenTelemetry instrumentation
- Static HTML templates

**After:**
- Node.js 22
- Next.js 15.5.4
- React 19.2.0
- TypeScript 5.9.3
- Full OpenTelemetry instrumentation (server & client)
- Modern React components with styled-components

### Key Features

1. **Modern Tech Stack**: Upgraded to latest versions of Next.js, React, and TypeScript
2. **OpenTelemetry Integration**: Complete observability with traces and metrics
3. **Backend Compatibility**: All existing backend service endpoints remain compatible
4. **Type Safety**: Full TypeScript support throughout the application

## Architecture

### Directory Structure

```
front-end/
├── pages/                  # Next.js pages and API routes
│   ├── _app.tsx           # Application wrapper with providers
│   ├── _document.tsx      # HTML document wrapper
│   ├── index.tsx          # Home page
│   └── api/               # API routes (proxy to backend services)
│       ├── cart.ts
│       ├── catalogue/
│       ├── orders.ts
│       ├── customers.ts
│       ├── addresses.ts
│       ├── cards.ts
│       ├── login.ts
│       ├── register.ts
│       └── tags.ts
├── utils/                 # Utility functions
│   ├── backend.ts         # Backend endpoint configuration
│   ├── telemetry/         # OpenTelemetry instrumentation
│   │   ├── Instrumentation.js
│   │   ├── FrontendTracer.ts
│   │   └── SessionIdProcessor.ts
│   └── enums/
├── providers/             # React context providers
│   ├── Cart.provider.tsx
│   └── Currency.provider.tsx
├── gateways/              # Service gateways
│   └── Session.gateway.ts
├── styles/                # Global styles and theme
│   ├── globals.css
│   └── Theme.ts
├── types/                 # TypeScript type definitions
├── components/            # React components (to be added)
└── public/                # Static assets
```

### Backend Compatibility

The application maintains full compatibility with existing Helidon backend services:

- **Catalogue Service**: `http://catalogue`
- **Carts Service**: `http://carts`
- **Orders Service**: `http://orders`
- **User Service**: `http://user`
- **Payment Service**: `http://payment`
- **Shipping Service**: `http://shipping`

All API routes in `pages/api/` act as proxies to these backend services, ensuring seamless integration.

### OpenTelemetry Instrumentation

#### Server-Side Instrumentation

The server uses `@opentelemetry/sdk-node` with automatic instrumentation:
- HTTP requests
- Database queries
- External API calls

Configuration: `utils/telemetry/Instrumentation.js`

#### Client-Side Instrumentation

The browser uses `@opentelemetry/sdk-trace-web` with:
- Fetch API instrumentation
- User interaction tracking
- Session ID tracking via custom `SessionIdProcessor`

Configuration: `utils/telemetry/FrontendTracer.ts`

#### Environment Variables

```bash
# OpenTelemetry Configuration
OTEL_COLLECTOR_HOST=otel-collector
WEB_OTEL_SERVICE_NAME=frontend
PUBLIC_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces
ENV_PLATFORM=local

# Backend Configuration
BACKEND_DOMAIN=
PORT=8080
```

## Development

### Prerequisites

- Node.js 22 or higher
- npm or yarn

### Installation

```bash
cd front-end
npm install
```

### Development Server

```bash
npm run dev
```

The application will be available at `http://localhost:8080`

### Building for Production

```bash
npm run build
```

### Running Production Build

```bash
npm start
```

### Linting

```bash
npm run lint
```

## Docker

### Building the Docker Image

```bash
docker build -t sockshop-frontend:latest .
```

### Running the Container

```bash
docker run -p 8080:8080 \
  -e BACKEND_DOMAIN=.default.svc.cluster.local \
  -e OTEL_COLLECTOR_HOST=otel-collector \
  sockshop-frontend:latest
```

## Migration Notes

### What Was Preserved

1. All backend API endpoints
2. Authentication flow
3. Session management
4. Cart functionality
5. Order processing
6. User management

### What Changed

1. UI framework (HTML templates → React components)
2. Routing (Express → Next.js)
3. Build process (simple npm start → Next.js build)
4. Added TypeScript
5. Added OpenTelemetry instrumentation
6. Modern dependency management

### Breaking Changes

- Port changed from 8079 to 8080 (configurable via PORT env var)
- Node version requirement updated from 10 to 22
- Build artifacts now in `.next/` directory instead of inline

## Testing

The application has been tested to ensure:
- ✅ Successful build with no TypeScript errors
- ✅ No ESLint warnings or errors
- ✅ All API routes properly configured
- ✅ OpenTelemetry instrumentation setup complete

## Future Enhancements

- [ ] Migrate UI components from static HTML to React components
- [ ] Add comprehensive unit tests
- [ ] Add integration tests
- [ ] Implement E2E tests with Cypress
- [ ] Add feature flags support with OpenFeature
- [ ] Enhance error handling and logging
- [ ] Add performance monitoring
- [ ] Implement proper authentication UI
- [ ] Add shopping cart UI components
- [ ] Implement order history UI

## Support

For questions or issues with the refactored frontend, please refer to:
- Next.js Documentation: https://nextjs.org/docs
- OpenTelemetry Documentation: https://opentelemetry.io/docs/
- React Documentation: https://react.dev/
