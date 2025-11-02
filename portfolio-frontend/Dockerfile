FROM node:20-alpine AS build

WORKDIR /app

# Install git for version generation (optional)
RUN apk add --no-cache git || true

COPY package*.json ./

RUN npm ci --legacy-peer-deps

# Copy all source code (including .git if present, excluded by .dockerignore if not needed)
COPY . .

RUN npm run build -- --configuration production

FROM nginx:alpine

COPY --from=build /app/dist/portfolio-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
