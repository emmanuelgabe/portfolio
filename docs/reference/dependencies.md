# Dependencies Reference

## Overview

This document explains how to view and manage dependencies in the Portfolio Application.

**For exact versions, see:**
- Backend: [`portfolio-backend/build.gradle`](../../portfolio-backend/build.gradle)
- Frontend: [`portfolio-frontend/package.json`](../../portfolio-frontend/package.json)

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