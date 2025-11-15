# Application Architecture Reference


---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)

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
| Backend | Spring Boot (Java) | 3.5.5 (Java 21) |
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
