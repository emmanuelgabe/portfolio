# Application Architecture Reference

Detailed architecture overview of the Portfolio application.

---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)
3. [Component Details](#3-component-details)
4. [Data Flow](#4-data-flow)
5. [Infrastructure Services](#5-infrastructure-services)

---

## 1. Overview

The Portfolio Application is a full-stack web application built with a modern three-tier architecture. The system uses containerization for deployment and follows microservices principles with clear separation of concerns.

### 1.1 Architecture Principles

**Design Principles**:
- Separation of concerns
- Stateless application servers
- Database as single source of truth
- Environment-specific configuration
- Container-based deployment
- Health check integration at all layers

### 1.2 Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | Angular | 18 LTS |
| Backend | Spring Boot (Java) | 3.4+ (Java 21) |
| Database | PostgreSQL | 17 |
| Cache | Redis | 7 |
| Message Queue | RabbitMQ | 3.12 |
| Event Streaming | Kafka | 3.5 |
| Search | Elasticsearch | 8.x |
| Reverse Proxy | Nginx | 1.27 |
| Containerization | Docker & Docker Compose | 20.10+ |
| CI/CD | GitHub Actions | N/A |

---

## 2. System Architecture

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                            CLIENT                                │
│                    (Web Browser / Mobile)                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ HTTPS/HTTP
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                         NGINX                                    │
│                   (Reverse Proxy / Load Balancer)                │
│  - SSL Termination                                              │
│  - Static File Serving                                          │
│  - Request Routing                                              │
│  - Health Check Aggregation                                     │
└──────────────┬─────────────────────────┬────────────────────────┘
               │                         │
               │ /                       │ /api/*
               │                         │
┌──────────────▼──────────┐   ┌──────────▼────────────────────────┐
│      FRONTEND           │   │         BACKEND                    │
│   Angular Application   │   │    Spring Boot Application         │
│  - SPA (Single Page)    │   │  - REST API + GraphQL API          │
│  - Responsive UI        │   │  - Business Logic                  │
│  - PWA Support          │   │  - Spring Security (JWT)           │
│  Port: 4200             │   │  - Spring Batch Jobs               │
└─────────────────────────┘   │  - Circuit Breaker (Resilience4j)  │
                              │  Port: 8080                         │
                              └────────────┬───────────────────────┘
                                           │
          ┌────────────────────────────────┼───────────────────────┐
          │                                │                       │
          ▼                                ▼                       ▼
┌─────────────────┐            ┌─────────────────┐    ┌─────────────────┐
│   POSTGRESQL    │            │     REDIS       │    │  ELASTICSEARCH  │
│   Port: 5432    │            │   Port: 6379    │    │   Port: 9200    │
│ - Data Storage  │            │ - Caching       │    │ - Full-text     │
│ - ACID          │            │ - Rate Limiting │    │   Search        │
│ - Audit Logs    │            │ - Sessions      │    │                 │
└─────────────────┘            └─────────────────┘    └─────────────────┘
          │
          ├────────────────────────────────────────────────────────┐
          │                                                        │
          ▼                                                        ▼
┌─────────────────────────┐                          ┌─────────────────┐
│       RABBITMQ          │                          │      KAFKA      │
│     Port: 5672/15672    │                          │   Port: 9092    │
│ - Email Queue           │                          │ - Admin Events  │
│ - Image Processing      │                          │ - Analytics     │
│ - Audit Logging         │                          │ - Activity      │
│ - Dead Letter Queue     │                          │                 │
└─────────────────────────┘                          └─────────────────┘
```

---

## 3. Component Details

### 3.1 Frontend (Angular)

**Responsibilities**:
- User interface rendering
- Client-side routing
- State management
- API communication
- PWA offline support

### 3.2 Backend (Spring Boot)

**Responsibilities**:
- REST and GraphQL API
- Business logic
- Authentication (JWT)
- Database operations
- Background jobs (Spring Batch)
- External service integration

### 3.3 Nginx

**Responsibilities**:
- SSL termination
- Static file serving
- API routing
- Load balancing
- Health check aggregation

---

## 4. Data Flow

### 4.1 Request Flow

```
Client → Nginx → Backend → Database
                   ↓
                 Redis (cache check)
                   ↓
              PostgreSQL (if not cached)
```

### 4.2 Event Flow

```
User Action → Backend Service → Kafka Producer
                                    ↓
                              Kafka Topic
                                    ↓
                             Kafka Consumer → Database/Analytics
```

### 4.3 Async Processing Flow

```
API Request → RabbitMQ Producer
                    ↓
              RabbitMQ Queue
                    ↓
              Consumer → SMTP/Image Processing/Audit
```

---

## 5. Infrastructure Services

### 5.1 Service Ports

| Service | Port | Protocol |
|---------|------|----------|
| Nginx | 80/443 | HTTP/HTTPS |
| Frontend | 4200 | HTTP |
| Backend | 8080 | HTTP |
| PostgreSQL | 5432 | TCP |
| Redis | 6379 | TCP |
| RabbitMQ | 5672 | AMQP |
| RabbitMQ Management | 15672 | HTTP |
| Kafka | 9092 | TCP |
| Elasticsearch | 9200 | HTTP |
| Prometheus | 9090 | HTTP |
| Grafana | 3001 | HTTP |
| Loki | 3100 | HTTP |

### 5.2 Service Dependencies

```
Backend depends on:
├── PostgreSQL (required)
├── Redis (required for rate limiting)
├── RabbitMQ (required for async)
├── Elasticsearch (optional, fallback to JPA)
└── Kafka (optional, feature flag)
```

---

## Related Documentation

- [Architecture README](./README.md) - Architecture documentation index
- [Database Schema](./database-schema.md) - PostgreSQL schema
- [Frontend Architecture](./frontend-architecture.md) - Angular architecture
- [Error Handling](./error-handling.md) - Exception handling
- [Messaging](./messaging.md) - RabbitMQ configuration
- [Event Streaming](./event-streaming.md) - Kafka configuration
