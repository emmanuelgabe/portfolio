# Architecture Documentation

---

## Table of Contents
1. [Introduction](#1-introduction)
2. [System Architecture](#2-system-architecture)
3. [Component Documentation](#3-component-documentation)

---

## 1. Introduction

This section provides documentation of the Portfolio application architecture. The application follows a modern three-tier architecture with Angular frontend, Spring Boot backend, and PostgreSQL database.

**Architecture Overview**:
- **Frontend**: Angular 18+ standalone components (SPA)
- **Backend**: Spring Boot 3.4+ REST API (Java 21)
- **Database**: PostgreSQL 17 with Flyway migrations
- **Messaging**: RabbitMQ for async processing
- **Event Streaming**: Apache Kafka for event-driven architecture
- **Search**: Elasticsearch for full-text search
- **Deployment**: Docker Compose containerization
- **Proxy**: Nginx reverse proxy

**Key Architectural Principles**:
- Separation of concerns (frontend/backend/database)
- Stateless application servers (JWT authentication)
- RESTful and GraphQL API design
- Event-driven architecture (Kafka)
- Asynchronous processing (RabbitMQ)
- Container-based deployment
- Environment-specific configuration

---

## 2. System Architecture

### 2.1 High-Level Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                         │
│                    (Browser, Mobile, API Consumers)                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           NGINX (Reverse Proxy)                              │
│                    SSL Termination, Load Balancing, Static Files             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
┌──────────────────────────────┐   ┌──────────────────────────────┐
│      ANGULAR FRONTEND        │   │      SPRING BOOT BACKEND     │
│         (Port 4200)          │   │         (Port 8080)          │
│                              │   │                              │
│  - Standalone Components     │   │  - REST API + GraphQL API    │
│  - PWA Support               │   │  - JWT Authentication        │
│  - i18n (10 languages)       │   │  - Spring Batch Jobs         │
│  - Chart.js Dashboards       │   │  - Circuit Breaker           │
└──────────────────────────────┘   └──────────────────────────────┘
                                                │
                    ┌───────────────────────────┼───────────────────────────┐
                    ▼                           ▼                           ▼
┌──────────────────────────┐   ┌──────────────────────────┐   ┌──────────────────────────┐
│      POSTGRESQL          │   │        REDIS             │   │     ELASTICSEARCH        │
│      (Port 5432)         │   │      (Port 6379)         │   │      (Port 9200)         │
│                          │   │                          │   │                          │
│  - 14 tables             │   │  - Rate Limiting         │   │  - Full-text Search      │
│  - Flyway V1-V25         │   │  - Session Cache         │   │  - Articles & Projects   │
│  - Audit Logs            │   │  - Visitor Tracking      │   │                          │
└──────────────────────────┘   └──────────────────────────┘   └──────────────────────────┘
                                                │
                    ┌───────────────────────────┼───────────────────────────┐
                    ▼                           ▼                           ▼
┌──────────────────────────┐   ┌──────────────────────────┐   ┌──────────────────────────┐
│       RABBITMQ           │   │        KAFKA             │   │      MONITORING          │
│      (Port 5672)         │   │      (Port 9092)         │   │                          │
│                          │   │                          │   │  - Prometheus (:9090)    │
│  - Email Queue           │   │  - Admin Events Topic    │   │  - Grafana (:3001)       │
│  - Image Processing      │   │  - Analytics Events      │   │  - Loki (:3100)          │
│  - Audit Logging         │   │  - Activity Events       │   │  - Sentry (cloud)        │
│  - Dead Letter Queue     │   │                          │   │                          │
└──────────────────────────┘   └──────────────────────────┘   └──────────────────────────┘
```

### 2.2 Key Components

| Component | Port | Purpose |
|-----------|------|---------|
| Nginx | 80/443 | Reverse proxy, SSL termination, static files |
| Angular SPA | 4200 | Frontend application |
| Spring Boot API | 8080 | Backend REST/GraphQL API |
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache, rate limiting, sessions |
| Elasticsearch | 9200 | Full-text search |
| RabbitMQ | 5672/15672 | Message queue |
| Kafka | 9092 | Event streaming |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3001 | Dashboards |
| Loki | 3100 | Log aggregation |

### 2.3 Environments

- **Local development**: Direct execution
- **Docker development**: docker-compose
- **Staging**: docker-compose with staging profile
- **Production**: Containerized deployment

---

## 3. Component Documentation

### 3.1 Frontend Architecture

**File**: [frontend-architecture.md](./frontend-architecture.md)

Angular 18+ architecture with standalone components, routing, state management, and HTTP communication.

**Topics**:
- Standalone components (no NgModules)
- Route configuration with nested admin routes
- Service-based state management
- HTTP interceptors (JWT, Retry, Logging)
- Functional guards (authGuard, adminGuard)

---

### 3.2 Backend Architecture

**Topics**: REST API design, service layer, security, validation

**Layers**:
```
Controller Layer → Service Layer → Repository Layer → Database
     ↓                 ↓
  Validation      Business Logic
```

**Key Components**:
- Controllers (`@RestController`) for JSON responses
- Services (`@Service`) with transaction management
- Repositories (JPA) with custom queries
- Spring Security with JWT authentication

---

### 3.3 Database Schema

**File**: [database-schema.md](./database-schema.md)

Complete PostgreSQL database schema with Flyway migrations.

**Topics**:
- 25 Flyway migrations (V1-V25)
- 14 database tables
- Entity relationships
- Indexes and constraints

---

### 3.4 Error Handling

**File**: [error-handling.md](./error-handling.md)

Centralized exception handling with GlobalExceptionHandler.

**Exception Mapping**:

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | Entity not found |
| `DuplicateResourceException` | 409 | Unique constraint violation |
| `MethodArgumentNotValidException` | 400 | Validation errors |
| `UnauthorizedException` | 401 | Invalid/expired JWT |
| `AccessDeniedException` | 403 | Insufficient permissions |
| `RateLimitExceededException` | 429 | Rate limit exceeded |

---

### 3.5 Messaging Architecture

**File**: [messaging.md](./messaging.md)

RabbitMQ-based asynchronous message processing.

**Key Components**:
- Portfolio Exchange (direct routing)
- Email, Image Processing, Audit queues
- Dead Letter Exchange for failed messages

---

### 3.6 Event Streaming Architecture

**File**: [event-streaming.md](./event-streaming.md)

Kafka-based event-driven architecture.

**Topics**:
- `admin.events` - Admin CRUD operations
- `analytics.events` - Page views, downloads
- `activity.events` - Login, session tracking

---

### 3.7 Caching Architecture

**File**: [../features/caching.md](../features/caching.md)

Multi-level caching for performance optimization.

| Layer | Technology | Purpose | TTL |
|-------|------------|---------|-----|
| L1 (Local) | Caffeine | In-memory cache | 5-10 min |
| L2 (Distributed) | Redis | Shared cache, sessions | 1-24 hours |

---

### 3.8 Resilience Architecture

**File**: [../features/circuit-breaker.md](../features/circuit-breaker.md)

Fault tolerance patterns for external service calls.

| Pattern | Use Case | Configuration |
|---------|----------|---------------|
| Circuit Breaker | External API calls | 5 failures, 60s wait |
| Retry | Transient failures | 3 retries, exponential backoff |
| Rate Limiter | API protection | Request limits per second |

---

## Related Documentation

- [API Documentation](../api/README.md) - REST API endpoints
- [Features Documentation](../features/README.md) - Feature implementations
- [Security: JWT Implementation](../security/jwt-implementation.md) - Authentication details
- [Security: RBAC](../security/rbac.md) - Role-based access control
- [Reference: Configuration Properties](../reference/configuration-properties.md) - Configuration
- [Development: Setup](../development/setup.md) - Development environment setup
- [Deployment: CI/CD](../deployment/ci-cd.md) - Deployment pipeline
