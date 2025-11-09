FROM node:20-alpine AS build

# Accept version as build argument
ARG VERSION=0.0.1-SNAPSHOT

WORKDIR /app

COPY package*.json ./

RUN npm ci --legacy-peer-deps

# Copy all source code
COPY . .

# Build with VERSION as environment variable (used by generate-version.js)
ENV VERSION=${VERSION}
RUN npm run build -- --configuration production

FROM nginx:alpine

# Install curl for health checks
RUN apk add --no-cache curl

COPY --from=build /app/dist/portfolio-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
