# Copyright The OpenTelemetry Authors
# SPDX-License-Identifier: Apache-2.0
# Express.js + OpenTelemetry Frontend

FROM docker.io/library/node:22-slim

WORKDIR /app

# Copy package files
COPY package.json package.json
COPY package-lock.json* package-lock.json*
COPY yarn.lock* yarn.lock*

# Install dependencies
RUN if [ -f package-lock.json ]; then npm ci --production; else npm install --production; fi

# Copy application files
COPY api/ api/
COPY config.js config.js
COPY helpers/ helpers/
COPY public/ public/
COPY scripts/ scripts/
COPY server-express.js server-express.js
COPY Instrumentation.js Instrumentation.js
COPY utils/ utils/

EXPOSE 8080

CMD ["node", "--require", "./Instrumentation.js", "server-express.js"]
