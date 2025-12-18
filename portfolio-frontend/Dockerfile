FROM node:20.18.1-alpine3.20 AS build

# Accept build arguments
ARG VERSION=0.0.1-SNAPSHOT
ARG SENTRY_DSN_FRONTEND=

WORKDIR /app

COPY package*.json ./

RUN npm ci --legacy-peer-deps

# Copy all source code
COPY . .

# Inject Sentry DSN into environment.prod.ts if provided
RUN if [ -n "$SENTRY_DSN_FRONTEND" ]; then \
      sed -i "s|dsn: ''|dsn: '$SENTRY_DSN_FRONTEND'|g" src/environments/environment.prod.ts; \
    fi

# Build with VERSION as environment variable (used by generate-version.js)
ENV VERSION=${VERSION}
RUN npm run build -- --configuration production

FROM nginx:1.27.3-alpine

# Install curl for health checks
RUN apk add --no-cache curl

COPY --from=build /app/dist/portfolio-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
