# Copyright The OpenTelemetry Authors
# SPDX-License-Identifier: Apache-2.0

FROM docker.io/library/node:22-slim AS builder

WORKDIR /app

COPY package.json package.json
COPY package-lock.json* package-lock.json*
COPY yarn.lock* yarn.lock*

RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY gateways/ gateways/
COPY pages/ pages/
COPY providers/ providers/
COPY styles/ styles/
COPY types/ types/
COPY utils/ utils/
COPY public/ public/
COPY next.config.js next.config.js
COPY tsconfig.json tsconfig.json
COPY Instrumentation.js Instrumentation.js

RUN npm run build

# -----------------------------------------------------------------------------

FROM docker.io/library/node:22-slim AS deps

WORKDIR /app

COPY package.json package.json
COPY package-lock.json* package-lock.json*
COPY yarn.lock* yarn.lock*

RUN if [ -f package-lock.json ]; then npm ci --omit=dev; else npm install --production; fi

# -----------------------------------------------------------------------------

FROM gcr.io/distroless/nodejs22-debian12:nonroot

WORKDIR /app

COPY --from=builder /app/.next/standalone/ ./
COPY --from=builder /app/.next/static/ .next/static/
COPY --from=deps /app/node_modules/ node_modules/
COPY --from=builder /app/public/ public/
COPY --from=builder /app/Instrumentation.js Instrumentation.js
COPY --from=builder /app/utils/ utils/
COPY --from=builder /app/gateways/ gateways/
COPY --from=builder /app/providers/ providers/
COPY --from=builder /app/types/ types/

EXPOSE 8080

CMD ["--require=./Instrumentation.js", "server.js"]
