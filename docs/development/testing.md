# Testing Guide

**Document Type:** Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active

---

## Table of Contents

1. [Overview](#1-overview)
2. [Quick Start](#2-quick-start)
3. [Test Coverage](#3-test-coverage)
4. [Testing Strategy](#4-testing-strategy)
5. [Continuous Integration](#5-continuous-integration)

---

## 1. Overview

Unit testing framework for the Portfolio Application ensures code quality and prevents regressions.

### 1.1 Testing Frameworks

| Component | Framework | Test Runner |
|-----------|-----------|-------------|
| Backend   | JUnit 5 + Mockito | Gradle |
| Frontend  | Jasmine | Karma |

### 1.2 Test Organization

```
docs/development/
├── testing.md                    # This file - overview and quick start
├── backend-tests.md              # Detailed backend testing guide
├── frontend-tests.md             # Detailed frontend testing guide
├── testing-quick-ref.md          # Quick reference commands
```

---

## 2. Quick Start

### 2.1 Run All Backend Tests

```bash
cd portfolio-backend
./gradlew test
```

**Report:** `portfolio-backend/build/reports/tests/test/index.html`

### 2.2 Run All Frontend Tests

```bash
cd portfolio-frontend
npm test -- --watch=false --browsers=ChromeHeadlessCI
```

**Coverage:** `portfolio-frontend/coverage/portfolio-frontend/index.html`

### 2.3 Common Commands

| Task | Command |
|------|---------|
| Backend: Specific test class | `./gradlew test --tests HealthServiceTest` |
| Frontend: With coverage | `npm test -- --code-coverage --watch=false --browsers=ChromeHeadlessCI` |
| Backend: Single test method | `./gradlew test --tests HealthServiceTest.testPing_ShouldReturnOkStatus` |
| Frontend: Interactive mode | `npm test` |

---

## 3. Test Coverage

### 3.1 Current Status

| Layer | Component | Tests | Status |
|-------|-----------|-------|--------|
| Backend | HealthService | 9 | PASS |
| Backend | HealthController | 8 | PASS |
| Backend | CustomHealthIndicator | 7 | PASS |
| Frontend | AppComponent | 11 | PASS |
| **Total** | **4 components** | **35** | **PASS** |

### 3.2 Coverage Metrics

- **Backend:** 100% of critical service classes
- **Frontend:** 100% of main application component
- **Overall:** Core health check functionality fully tested

### 3.3 Coverage Goals

| Layer | Minimum Coverage |
|-------|------------------|
| Critical paths | 100% |
| Service layer | 90%+ |
| Controllers/Components | 80%+ |
| Utilities | 70%+ |

---

## 4. Testing Strategy

### 4.1 Test Pattern (AAA)

All tests follow the Arrange-Act-Assert pattern:

```java
@Test
void testPing_ShouldReturnOkStatus() {
    // Arrange: Setup mocks and test data
    // Act: Execute the method under test
    HealthResponse response = healthService.ping();
    // Assert: Verify the results
    assertNotNull(response);
    assertEquals("ok", response.getStatus());
}
```

### 4.2 Naming Convention

Pattern: `test<Method>_When<Condition>_Should<ExpectedBehavior>`

**Examples:**
- `testPing_ShouldReturnOkStatus()`
- `testCheckDatabase_WhenHealthy_ShouldReturn200()`
- `should create the component`
- `should render title when component initializes`

### 4.3 Best Practices

**Test Independence:**
- Run independently of other tests
- No reliance on execution order
- Clean up resources after execution
- No shared state between tests

**Mock Usage:**
- Use lenient stubs for optional dependencies
- Spy on external dependencies when needed
- Verify interactions when behavior matters

---

## 5. Continuous Integration

### 5.1 CI/CD Integration

Tests are integrated into GitHub Actions workflow (`.github/workflows/ci-cd.yml`).

**Automated execution:**
- On every pull request
- Before deployment to staging/production
- On scheduled intervals (nightly builds)

### 5.2 Enabling Tests in Pipeline

**Backend:**
```yaml
- name: Run Backend Unit Tests
  run: ./gradlew test
  working-directory: portfolio-backend
```

**Frontend:**
```yaml
- name: Run Frontend Unit Tests
  run: npm test -- --watch=false --browsers=ChromeHeadlessCI
  working-directory: portfolio-frontend
```

**Note:** Tests are currently disabled in pipeline but can be enabled by uncommenting these steps.

---



## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active
