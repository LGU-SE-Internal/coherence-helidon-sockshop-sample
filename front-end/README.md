# Sock Shop Frontend

Modern frontend application for the Sock Shop microservices demo, built with Next.js, React 19, TypeScript, and full OpenTelemetry observability support.

## Overview

This is a completely refactored version of the Sock Shop frontend, upgraded from a legacy Express.js application to a modern Next.js application while maintaining full backward compatibility with existing backend services.

### Key Features

- **Next.js 15.5.4** - Modern React framework with server-side rendering
- **React 19.2.0** - Latest React with concurrent features
- **TypeScript 5.9.3** - Full type safety throughout the application
- **OpenTelemetry** - Complete observability with distributed tracing and metrics
- **Backend Compatible** - Works seamlessly with existing Helidon microservices
- **Styled Components** - Modern CSS-in-JS styling solution

### Technology Stack

- **Framework**: Next.js 15.5.4
- **UI Library**: React 19.2.0
- **Language**: TypeScript 5.9.3
- **Observability**: OpenTelemetry SDK (Node & Web)
- **Styling**: Styled Components 6.1.19
- **State Management**: React Query 5.90.2

## 📖 Documentation

For detailed migration information, see [MIGRATION.md](./MIGRATION.md)

## Prerequisites

- Node.js 22 or higher
- npm or yarn
- Docker (for containerized deployment)

## Quick Start

### Local Development

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Application will be available at http://localhost:8080
```

### Build for Production

```bash
# Build the application
npm run build

# Start production server
npm start
```

### Linting

```bash
npm run lint
```

## Docker Deployment

### Build Docker Image

```bash
docker build -t sockshop-frontend:latest .
```

### Run Container

```bash
docker run -p 8080:8080 \
  -e BACKEND_DOMAIN=.default.svc.cluster.local \
  -e OTEL_COLLECTOR_HOST=otel-collector \
  -e WEB_OTEL_SERVICE_NAME=frontend \
  sockshop-frontend:latest
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8080` |
| `BACKEND_DOMAIN` | Backend service domain suffix | `` |
| `OTEL_COLLECTOR_HOST` | OpenTelemetry collector host | `otel-collector` |
| `WEB_OTEL_SERVICE_NAME` | Service name for traces | `frontend` |
| `PUBLIC_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | OTLP traces endpoint | `http://localhost:4318/v1/traces` |
| `ENV_PLATFORM` | Platform identifier | `local` |

## Backend Services

The frontend connects to the following backend microservices:

- **Catalogue**: Product catalog and search
- **Carts**: Shopping cart management
- **Orders**: Order processing
- **User**: User authentication and management
- **Payment**: Payment processing
- **Shipping**: Shipping calculations

All API routes in `pages/api/` proxy requests to these backend services, maintaining full compatibility.

## OpenTelemetry Observability

This application includes comprehensive OpenTelemetry instrumentation:

### Server-Side Tracing
- HTTP request/response tracing
- Database query tracing
- External API call tracing
- Custom span attributes

### Client-Side Tracing
- Fetch API instrumentation
- User interaction tracking
- Page navigation tracking
- Session ID correlation

### Metrics
- Request counters
- Response times
- Error rates
- Custom business metrics

Traces and metrics are exported to the configured OTLP endpoint for visualization in tools like Jaeger, Zipkin, or Grafana.

## Project Structure

```
front-end/
├── pages/              # Next.js pages and API routes
├── components/         # React components
├── providers/          # React context providers
├── utils/              # Utility functions
├── styles/             # Global styles and themes
├── types/              # TypeScript type definitions
├── gateways/           # Service gateways
└── public/             # Static assets
```

## API Routes

All API routes maintain compatibility with the original Express.js endpoints:

- `GET /api/catalogue` - Get product catalogue
- `GET /api/tags` - Get product tags
- `GET /api/cart` - Get cart items
- `POST /api/cart` - Add item to cart
- `DELETE /api/cart` - Clear cart
- `GET /api/orders` - Get orders
- `POST /api/orders` - Create order
- `GET /api/customers` - Get customer info
- `POST /api/login` - User login
- `POST /api/register` - User registration
- `GET /api/addresses` - Get addresses
- `POST /api/addresses` - Add address
- `GET /api/cards` - Get payment cards
- `POST /api/cards` - Add payment card

## License

This project maintains the original MIT license from the Sock Shop microservices demo.
