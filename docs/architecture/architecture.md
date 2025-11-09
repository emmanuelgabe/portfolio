# Application Architecture Reference

**Document Type:** Technical Reference
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active

---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)
3. [Component Architecture](#3-component-architecture)
4. [Data Flow](#4-data-flow)
5. [Infrastructure Architecture](#5-infrastructure-architecture)
6. [Security Architecture](#6-security-architecture)
7. [Deployment Architecture](#7-deployment-architecture)

---

## 1. Overview

The Portfolio Application is a full-stack web application built with a modern three-tier architecture. The system uses containerization for deployment and follows microservices principles with clear separation of concerns.

### 1.1 Architecture Principles

**Design Principles:**
- Separation of concerns
- Stateless application servers
- Database as single source of truth
- Environment-specific configuration
- Container-based deployment
- Health check integration at all layers

**Technology Stack:**

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | Angular | 20.0.0 |
| Backend | Spring Boot (Java) | 3.5.5 (Java 24) |
| Database | PostgreSQL | 17.6 |
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
│  • SSL Termination                                              │
│  • Static File Serving                                          │
│  • Request Routing                                              │
│  • Health Check Aggregation                                     │
└──────────────┬─────────────────────────┬────────────────────────┘
               │                         │
               │ /                       │ /api/*
               │                         │
┌──────────────▼──────────┐   ┌──────────▼────────────────────────┐
│      FRONTEND           │   │         BACKEND                    │
│   Angular Application   │   │    Spring Boot Application         │
│  • SPA (Single Page)    │   │  • REST API                        │
│  • Responsive UI        │   │  • Business Logic                  │
│  • Health Checks        │   │  • Security (Spring Security)      │
│  Port: 4200             │   │  • Data Access Layer               │
└─────────────────────────┘   │  • Health Checks (Actuator)        │
                              │  Port: 8080                         │
                              └────────────┬───────────────────────┘
                                           │
                                           │ JDBC
                                           │
                              ┌────────────▼───────────────────────┐
                              │        POSTGRESQL                   │
                              │   Relational Database               │
                              │  • Data Persistence                 │
                              │  • ACID Transactions                │
                              │  Port: 5432                         │
                              └─────────────────────────────────────┘
```

### 2.2 Request Flow

**Static Content Request:**
```
Client → Nginx → Frontend (Angular) → Client
```

**API Request:**
```
Client → Nginx → Backend → Database → Backend → Nginx → Client
```

**Health Check Request:**
```
Client → Nginx → Aggregated Health Status → Client
       → Frontend (health.json)
       → Backend (actuator/health)
       → Database (pg_isready)
```

---

## 3. Component Architecture

### 3.1 Frontend Architecture (Angular)

```
portfolio-frontend/
├── src/
│   ├── app/
│   │   ├── app.component.ts           # Root component
│   │   ├── app.component.html         # Root template
│   │   ├── app.component.css          # Root styles
│   │   └── app.config.ts              # Application configuration
│   ├── environments/
│   │   └── version.ts                 # Auto-generated version
│   ├── assets/
│   │   └── health.json                # Static health check file
│   ├── index.html                     # Entry point
│   └── main.ts                        # Bootstrap file
├── angular.json                       # Angular CLI configuration
├── package.json                       # Dependencies
├── proxy.conf.json                    # Development proxy
└── Dockerfile                         # Container build instructions
```

**Frontend Layers:**

```
┌─────────────────────────────────────┐
│        Presentation Layer            │
│  • Components (Angular)              │
│  • Templates (HTML)                  │
│  • Styles (CSS/Bootstrap)            │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Service Layer                │
│  • HTTP Services                     │
│  • State Management                  │
│  • Business Logic                    │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Communication Layer             │
│  • HTTP Client (RxJS)                │
│  • REST API Calls                    │
│  • Error Handling                    │
└──────────────────────────────────────┘
```

### 3.2 Backend Architecture (Spring Boot)

```
portfolio-backend/
├── src/
│   ├── main/
│   │   ├── java/com/emmanuelgabe/portfolio/
│   │   │   ├── PortfolioBackendApplication.java   # Main entry point
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java           # Security configuration
│   │   │   ├── controller/
│   │   │   │   └── HealthController.java         # REST controllers
│   │   │   ├── service/
│   │   │   │   └── HealthService.java            # Business logic
│   │   │   ├── health/
│   │   │   │   └── CustomHealthIndicator.java    # Custom health checks
│   │   │   └── dto/
│   │   │       └── HealthResponse.java           # Data transfer objects
│   │   └── resources/
│   │       ├── application.yml                   # Base configuration
│   │       ├── application-dev.yml               # Dev configuration
│   │       ├── application-staging.yml           # Staging configuration
│   │       └── application-prod.yml              # Production configuration
│   └── test/                                     # Unit tests
├── build.gradle                                  # Build configuration
└── Dockerfile                                    # Container build instructions
```

**Backend Layers:**

```
┌─────────────────────────────────────┐
│       Controller Layer               │
│  • REST Endpoints (@RestController) │
│  • Request Mapping                   │
│  • Input Validation                  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Service Layer                 │
│  • Business Logic (@Service)         │
│  • Transaction Management            │
│  • Data Transformation               │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│    Data Access Layer (Future)        │
│  • Repository (@Repository)          │
│  • JPA Entities                      │
│  • Database Queries                  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Database Layer               │
│  • PostgreSQL                        │
│  • ACID Transactions                 │
│  • Data Persistence                  │
└──────────────────────────────────────┘
```

### 3.3 Database Architecture

**Current Schema:**

```
PostgreSQL Database: portfolio_{env}
├── Schema: public
│   └── (Tables to be defined)
└── Connection Pool
    ├── Min Connections: 5
    ├── Max Connections: 20
    └── Idle Timeout: 10 minutes
```

**Database Configuration by Environment:**

| Environment | Database Name | Port | Max Connections |
|-------------|---------------|------|-----------------|
| Local | `portfolio_local` | 5432 | 20 |
| Staging | `portfolio_staging` | 5434 | 50 |
| Production | `portfolio_prod` | 5432 | 100 |

---

## 4. Data Flow

### 4.1 Application Startup Flow

```
1. Docker Compose starts containers
   │
   ├─→ PostgreSQL starts first
   │   └─→ Health check: pg_isready
   │
   ├─→ Backend starts (depends on DB)
   │   ├─→ Spring Boot initialization
   │   ├─→ Database connection pool setup
   │   ├─→ Spring Security configuration
   │   ├─→ Actuator endpoints activation
   │   └─→ Health check: /actuator/health
   │
   ├─→ Frontend starts
   │   ├─→ Version generation (git describe)
   │   ├─→ Angular build
   │   ├─→ Nginx configuration
   │   └─→ Health check: /health.json
   │
   └─→ Nginx starts
       ├─→ Load configuration
       ├─→ Upstream health verification
       └─→ Ready to accept requests
```

### 4.2 API Request Flow

```
1. Client sends HTTP request
   │
2. Nginx receives request
   ├─→ Log request
   ├─→ Apply rate limiting (if configured)
   ├─→ Check route: / vs /api/*
   │
3a. Static route (/)
   └─→ Serve from Frontend container
       └─→ Return Angular SPA

3b. API route (/api/*)
   └─→ Forward to Backend container
       │
       4. Spring Security Filter Chain
       ├─→ CORS validation
       ├─→ CSRF validation (staging/prod)
       ├─→ Authentication (if required)
       └─→ Authorization (if required)
       │
       5. Controller Layer
       ├─→ Route to appropriate controller
       ├─→ Validate input parameters
       └─→ Call service layer
       │
       6. Service Layer
       ├─→ Execute business logic
       ├─→ Call database (if needed)
       └─→ Transform data to DTO
       │
       7. Return response
       ├─→ Serialize to JSON
       ├─→ Add response headers
       └─→ Send to Nginx
   │
8. Nginx forwards response to client
```

### 4.3 Health Check Flow

```
External Health Check Request
   │
   ├─→ Nginx: /health
   │   └─→ Returns 200 OK (simple check)
   │
   ├─→ Nginx: /health/full
   │   ├─→ Calls Frontend: /health.json
   │   │   └─→ Returns static JSON with version
   │   │
   │   └─→ Calls Backend: /api/health/status
   │       ├─→ Checks Backend API: /api/health/ping
   │       └─→ Checks Database: /api/health/db
   │           └─→ PostgreSQL: SELECT 1
   │
   └─→ Aggregates all results
       └─→ Returns comprehensive health status
```

---

## 5. Infrastructure Architecture

### 5.1 Container Architecture

```
Docker Network: portfolio-network
│
├─→ Container: portfolio-nginx-{env}
│   ├─→ Image: nginx:1.27-alpine
│   ├─→ Ports: {env_port}:80
│   ├─→ Volumes: ./nginx/conf.d
│   └─→ Depends on: frontend, backend
│
├─→ Container: portfolio-frontend-{env}
│   ├─→ Image: Custom (multi-stage build)
│   │   ├─→ Stage 1: Node.js build
│   │   └─→ Stage 2: Nginx serve
│   ├─→ Port: 4200 (internal)
│   ├─→ Healthcheck: wget /health.json
│   └─→ Volumes: none (self-contained)
│
├─→ Container: portfolio-backend-{env}
│   ├─→ Image: Custom (multi-stage build)
│   │   ├─→ Stage 1: Gradle build
│   │   └─→ Stage 2: JRE runtime
│   ├─→ Port: 8080 (internal)
│   ├─→ Healthcheck: wget /actuator/health
│   ├─→ Depends on: database
│   └─→ Environment: DB credentials
│
└─→ Container: portfolio-db-{env}
    ├─→ Image: postgres:17.6-alpine
    ├─→ Port: 5432 (local), 5434 (staging)
    ├─→ Healthcheck: pg_isready
    ├─→ Volume: db_data (persistent)
    └─→ Environment: DB credentials
```

### 5.2 Network Architecture

**Docker Network Configuration:**

```
Network: portfolio-network (bridge mode)
├─→ Subnet: 172.20.0.0/16
└─→ Container IPs (dynamic assignment)
    ├─→ Nginx: 172.20.0.2
    ├─→ Frontend: 172.20.0.3
    ├─→ Backend: 172.20.0.4
    └─→ Database: 172.20.0.5
```

**Port Mapping by Environment:**

| Environment | Nginx | Backend | Frontend | Database |
|-------------|-------|---------|----------|----------|
| Local | 8081→80 | 8080 (internal) | 4200 (internal) | 5432→5432 |
| Staging | 3000→80 | 8080 (internal) | 4200 (internal) | 5434→5432 |
| Production | 80→80 | 8080 (internal) | 4200 (internal) | 5432 (internal) |

### 5.3 Volume Architecture

**Persistent Volumes:**

```
Volumes:
├─→ {env}_db_data
│   ├─→ Type: Named volume
│   ├─→ Mount: /var/lib/postgresql/data
│   └─→ Purpose: Database persistence
│
└─→ .git (copied during deployment)
    ├─→ Type: Bind mount
    ├─→ Mount: /app/.git
    └─→ Purpose: Version generation
```

---

## 6. Security Architecture

### 6.1 Security Layers

```
┌─────────────────────────────────────────────────┐
│              Network Security                    │
│  • Firewall rules                               │
│  • Port restrictions                            │
│  • Internal container network                   │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│           Nginx Security                         │
│  • Rate limiting (if configured)                │
│  • Request size limits                          │
│  • Header filtering                             │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│        Spring Security Layer                     │
│  • CORS configuration                           │
│  • CSRF protection (staging/prod)               │
│  • Authentication (future)                      │
│  • Authorization (future)                       │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│        Application Security                      │
│  • Input validation                             │
│  • SQL injection prevention (JPA)               │
│  • XSS prevention (Angular sanitization)        │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│         Database Security                        │
│  • User authentication                          │
│  • Role-based access                            │
│  • Encrypted connections                        │
└──────────────────────────────────────────────────┘
```

### 6.2 Security Configuration by Environment

| Security Feature | Local | Staging | Production |
|------------------|-------|---------|------------|
| HTTPS | No | Planned | Required |
| CSRF Protection | Disabled | Enabled | Enabled |
| CORS | Permissive | Restrictive | Restrictive |
| Security Headers | Minimal | Standard | Full |
| Database Encryption | No | Planned | Required |

---

## 7. Deployment Architecture

### 7.1 CI/CD Pipeline Architecture

```
┌─────────────────────────────────────────────────┐
│           GitHub Repository                      │
│  • Source code                                  │
│  • Dockerfile configurations                    │
│  • Docker Compose files                         │
└──────────────┬──────────────────────────────────┘
               │
               │ Push/Merge
               │
┌──────────────▼──────────────────────────────────┐
│         GitHub Actions (CI/CD)                   │
│  Job 1: Build and Test                          │
│  ├─→ Checkout code                              │
│  ├─→ Extract version from Git tags              │
│  ├─→ Build backend (Gradle)                     │
│  └─→ Run tests (disabled currently)             │
│                                                  │
│  Job 2: Deploy (main/staging only)              │
│  ├─→ Copy files to deployment server            │
│  ├─→ Build Docker images                        │
│  ├─→ Start containers (Docker Compose)          │
│  ├─→ Wait for health checks                     │
│  ├─→ Tag Docker images with version             │
│  └─→ Cleanup old resources                      │
└──────────────┬──────────────────────────────────┘
               │
               │ Deploy
               │
┌──────────────▼──────────────────────────────────┐
│       Deployment Server (Self-hosted)            │
│  • GitHub Actions Runner                        │
│  • Docker Engine                                │
│  • Application containers                       │
└──────────────────────────────────────────────────┘
```

### 7.2 Deployment Environments

**Directory Structure on Deployment Server:**

```
/home/manu/projects/portfolio/
├── prod/                              # Production deployment
│   ├── docker-compose.yml
│   ├── docker-compose.prod.yml
│   ├── .git/                          # For version generation
│   ├── portfolio-backend/
│   ├── portfolio-frontend/
│   └── nginx/
│
└── stage/                             # Staging deployment
    ├── docker-compose.yml
    ├── docker-compose.staging.yml
    ├── .git/                          # For version generation
    ├── portfolio-backend/
    ├── portfolio-frontend/
    └── nginx/
```

### 7.3 Scaling Architecture (Future)

**Horizontal Scaling Strategy:**

```
┌─────────────────────────────────────┐
│         Load Balancer                │
│         (Nginx / HAProxy)            │
└──────┬──────────────┬────────────────┘
       │              │
   ┌───▼────┐    ┌───▼────┐
   │Backend │    │Backend │
   │Instance│    │Instance│
   │   1    │    │   2    │
   └───┬────┘    └───┬────┘
       │              │
       └──────┬───────┘
              │
      ┌───────▼────────┐
      │   PostgreSQL   │
      │  (Primary)     │
      └────────────────┘
```

---

## 8. Performance Considerations

### 8.1 Backend Optimization

**JVM Configuration:**
```
-Xms256m                  # Initial heap size
-Xmx512m                  # Maximum heap size
-XX:MaxMetaspaceSize=128m # Metaspace limit
```

**Connection Pool:**
```
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
```

### 8.2 Frontend Optimization

**Build Optimization:**
- Production build with AOT compilation
- Tree shaking for unused code removal
- Lazy loading for routes (future)
- Asset compression (gzip/brotli)

**Docker Multi-stage Build:**
- Stage 1: Build with Node.js (reduced build time)
- Stage 2: Serve with Nginx (minimal runtime image)
- Final image size: ~50MB (vs ~800MB without optimization)

### 8.3 Database Optimization

**Indexing Strategy (Future):**
- Primary keys on all tables
- Foreign key indexes
- Composite indexes for common queries

**Query Optimization:**
- Use JPA query optimization
- Connection pooling
- Prepared statements

---

## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Technical Reference
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active
