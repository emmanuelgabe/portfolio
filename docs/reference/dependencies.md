# Dependencies Reference

## Overview

This document explains how to view and manage dependencies in the Portfolio Application.

**For exact versions, see:**
- Backend: `portfolio-backend/build.gradle`
- Frontend: `portfolio-frontend/package.json`

**View all dependencies:**
```bash
# Backend
cd portfolio-backend && ./gradlew dependencies

# Frontend
cd portfolio-frontend && npm list
```


---

## Backend Stack

### Core Framework
- **Spring Boot 3.x** - Application framework with starters:
  - `spring-boot-starter-web` - REST API, MVC, Tomcat
  - `spring-boot-starter-data-jpa` - JPA/Hibernate
  - `spring-boot-starter-security` - Auth
  - `spring-boot-starter-actuator` - Health checks, metrics
  - `spring-boot-starter-validation` - Bean validation
- **Java 21 LTS** - Programming language
- **Gradle 8.x** - Build tool

### Database
- **PostgreSQL** - Production database
- **Flyway** - Database migrations

### API Documentation
- **SpringDoc OpenAPI** - Swagger UI auto-generation

**Access:** http://localhost:8081/api/swagger-ui.html

### Code Generation
- **MapStruct** - Compile-time DTO mapping
- **Lombok** - Boilerplate reduction (@Getter, @Slf4j)
- **Lombok-MapStruct Binding** - Compatibility layer

**Why:** Compile-time mapping faster than reflection-based alternatives.

### Logging
- **Logstash Logback Encoder** - Structured JSON logs

### Messaging
- **spring-boot-starter-amqp** - RabbitMQ integration
- **spring-kafka** - Apache Kafka integration

### Search
- **spring-boot-starter-data-elasticsearch** - Elasticsearch integration

### Batch Processing
- **spring-boot-starter-batch** - Scheduled batch jobs

### Caching
- **spring-boot-starter-cache** - Cache abstraction
- **Caffeine** - High-performance local cache

### Resilience
- **resilience4j-spring-boot3** - Circuit breaker, retry patterns
- **resilience4j-micrometer** - Metrics integration

### GraphQL
- **spring-boot-starter-graphql** - GraphQL API support

### Testing
- **Spring Boot Test** - JUnit 5, Mockito, AssertJ
- **REST Assured** - REST API testing
- **Testcontainers** - Real database in tests (no H2 mocks)

## Frontend Stack

### Core Framework
- **Angular 18 LTS** - Framework with standalone components
- **TypeScript 5.x** - Programming language
- **RxJS 7.x** - Reactive programming
- **Zone.js** - Change detection

### UI Framework
- **Bootstrap 5.x** - CSS framework, responsive grid
- **Bootstrap Icons** - Icon library
- **ngx-toastr** - Toast notifications

### Utilities
- **date-fns** - Modern date library

### Charts and Visualization
- **chart.js** - Charting library
- **ng2-charts** - Angular Chart.js wrapper

### PWA (Progressive Web App)
- **@angular/service-worker** - Service worker support

### Internationalization (i18n)
- **@ngx-translate/core** - Translation core
- **@ngx-translate/http-loader** - JSON translation loader

### Performance Monitoring
- **web-vitals** - Core Web Vitals metrics

### Development Tools
- **ESLint + Prettier** - Linting and formatting
- **Husky + lint-staged** - Git hooks for automated checks

### Testing
- **Jasmine + Karma** - Testing framework and runner
- **Karma Coverage** - Code coverage reports

## Infrastructure

### Containerization
- **Docker** - Container runtime
- **Docker Compose** - Multi-container orchestration

**Base Images (Alpine for minimal size):**
- `node:20-alpine` - Frontend build
- `nginx:alpine` - Frontend runtime, reverse proxy
- `eclipse-temurin:21-jdk-alpine` - Backend build
- `eclipse-temurin:21-jre-alpine` - Backend runtime
- `postgres:17-alpine` - Database

### Messaging Infrastructure
- **RabbitMQ** (`rabbitmq:3-management-alpine`) - Message broker
- **Apache Kafka** + Zookeeper - Event streaming

### Search Infrastructure
- **Elasticsearch** - Full-text search engine

### Monitoring Stack
- **Prometheus** - Metrics collection
- **Grafana** - Dashboards and visualization
- **Loki** - Log aggregation
- **Promtail** - Log collector

### Cache Infrastructure
- **Redis** - Distributed cache and rate limiting

---

## Related Documentation

- [Observability](../operations/observability.md) - Monitoring stack details
- [Environments](./environments.md) - Service URLs by environment
- [Configuration Properties](./configuration-properties.md) - All configuration options