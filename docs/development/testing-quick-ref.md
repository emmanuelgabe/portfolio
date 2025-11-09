# Testing Quick Reference

**Document Type:** Quick Reference
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active

---

## Backend Tests (Gradle)

| Task | Command |
|------|---------|
| Run all tests | `./gradlew test` |
| Run specific test class | `./gradlew test --tests HealthServiceTest` |
| Run single test method | `./gradlew test --tests HealthServiceTest.testPing_ShouldReturnOkStatus` |
| Clean and test | `./gradlew clean test` |
| Test with info logging | `./gradlew test --info` |

**Test Report:** `portfolio-backend/build/reports/tests/test/index.html`

---

## Frontend Tests (npm)

| Task | Command |
|------|---------|
| Run all tests (headless) | `npm test -- --watch=false --browsers=ChromeHeadlessCI` |
| Run tests (interactive) | `npm test` |
| Run with coverage | `npm test -- --code-coverage --watch=false --browsers=ChromeHeadlessCI` |
| Run in watch mode | `npm test -- --watch=true` |

**Coverage Report:** `portfolio-frontend/coverage/portfolio-frontend/index.html`

---

## Test Naming Convention

**Pattern:** `test<Method>_When<Condition>_Should<ExpectedBehavior>`

**Backend Examples:**
```java
testPing_ShouldReturnOkStatus()
testCheckDatabase_WhenHealthy_ShouldReturn200()
testGetStatus_WhenUnhealthy_ShouldReturn503()
```

**Frontend Examples:**
```typescript
it('should create the component', () => { ... })
it('should render title when component initializes', () => { ... })
it('should display version number', () => { ... })
```

---

## AAA Test Pattern

```java
@Test
void testMethod() {
    // Arrange: Setup mocks and test data

    // Act: Execute method under test

    // Assert: Verify results
}
```

---

## Common Annotations

**Backend (JUnit 5 + Mockito):**
```java
@ExtendWith(MockitoExtension.class)  // Enable Mockito
@Mock                                 // Create mock
@InjectMocks                          // Inject mocks into this object
@BeforeEach                           // Run before each test
@Test                                 // Mark as test method
@WebMvcTest(Controller.class)        // Test web layer
@AutoConfigureMockMvc(addFilters = false)  // Disable security
@ActiveProfiles("test")              // Use test profile
```

**Frontend (Jasmine/Karma):**
```typescript
describe('TestSuite', () => { ... })     // Test suite
beforeEach(() => { ... })                // Run before each test
it('should ...', () => { ... })          // Test case
expect(value).toBeTruthy()               // Assertion
spyOn(object, 'method')                  // Create spy
```

---

## CI/CD Integration

**Enable tests in `.github/workflows/ci-cd.yml`:**

```yaml
# Backend
- name: Run Backend Unit Tests
  run: ./gradlew test
  working-directory: portfolio-backend

# Frontend
- name: Run Frontend Unit Tests
  run: npm test -- --watch=false --browsers=ChromeHeadlessCI
  working-directory: portfolio-frontend
```

---

**Document Type:** Quick Reference
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active
