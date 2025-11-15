# Testing Guide

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

### 3.1 View Current Coverage

**Backend:**
```bash
cd portfolio-backend
./gradlew test jacocoTestReport
# Open: build/reports/jacoco/test/html/index.html
```

**Frontend:**
```bash
cd portfolio-frontend
npm test -- --code-coverage --watch=false
# Open: coverage/portfolio-frontend/index.html
```

### 3.2 Coverage Requirements

Coverage thresholds are enforced by CI/CD.

**Backend configuration:** See `portfolio-backend/build.gradle` lines 136-167
**Frontend configuration:** See `karma.conf.js` (if configured)

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
