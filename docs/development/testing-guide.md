# Testing Guide

Complete guide for testing strategies, conventions, and execution in the Portfolio project.

---

## Table of Contents

1. [Test Maintenance Rule](#test-maintenance-rule)
2. [Backend Testing](#backend-testing)
3. [Frontend Testing](#frontend-testing)
4. [Test Naming Conventions](#test-naming-conventions)
5. [Test Execution](#test-execution)
6. [Coverage Requirements](#coverage-requirements)

---

## Test Maintenance Rule

### CRITICAL: Mandatory Test Updates

**For EVERY code change, you MUST verify and update tests BEFORE committing.**

**[REQUIRED] Before completing ANY task:**
1. **Identify affected tests** - Which test files cover the modified code?
2. **Update test expectations** - Do assertions match new behavior?
3. **Remove obsolete tests** - Are any tests now invalid?
4. **Add missing tests** - Does new code need new tests?
5. **Run tests locally** - Verify all tests pass before pushing

---

## Backend Testing

### Testing Strategy

#### @MockBean Usage Policy (Spring Boot 3.4+)

**CURRENT POLICY:**
- **USE** `@MockBean` from `org.springframework.boot.test.mock.mockito` for ALL tests
- This is the REQUIRED annotation for this project
- [WARN] Expect deprecation warnings during compilation (this is normal and acceptable)

**RATIONALE:**
- `@MockBean` is deprecated since Spring Boot 3.4 but remains fully functional until Spring Boot 4.0
- The replacement `@MockitoBean` does NOT work with `@TestConfiguration` in Spring Boot 3.4+
- Migration to `@MockitoBean` would require:
  - Removing shared `TestSecurityConfig.java`
  - Duplicating 7+ mock beans across every test file
  - Massive refactoring effort (~15-20 hours)
- Current approach is stable, maintainable, and will work until Spring Boot 4.0 (estimated 2026-2027)

### Unit Tests (JUnit 5 + Mockito)

**Characteristics:**
- Test business logic in isolation
- Mock all external dependencies
- Fast execution (no Docker required)
- Target: >80% code coverage

**Pattern:**
```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Repository repository;

    @Mock
    private Mapper mapper;

    @InjectMocks
    private Service service;

    @Test
    void should_<behavior>_when_<condition>() {
        // Given: Setup test data and mocks
        // When: Execute method under test
        // Then: Verify results and interactions
    }
}
```

### Integration Tests (Testcontainers)

**Characteristics:**
- Test with real database
- Verify JPA mappings and queries
- Test transactions and constraints
- Docker required

**Pattern:**
```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private Repository repository;

    @Test
    void should_<behavior>_when_<condition>() {
        // Test with real database operations
    }
}
```

### API Tests (REST Assured)

**Characteristics:**
- Test complete HTTP flows
- Verify request/response formats
- Test authentication and authorization

**Pattern:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void should_<behavior>_when_<condition>() {
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/endpoint")
        .then()
            .statusCode(201)
            .body("field", equalTo("value"));
    }
}
```

---

## Frontend Testing

### Unit Tests (Jasmine + Karma)

**Characteristics:**
- Test component logic in isolation
- Mock services and dependencies
- Fast execution

### Configuration Files

**karma.conf.js:**
```javascript
module.exports = function(config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    browsers: ['Chrome'],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox']
      }
    }
  });
};
```

**tsconfig.spec.json:**
```json
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "outDir": "./out-tsc/spec",
    "types": ["jasmine"]
  },
  "include": ["src/**/*.spec.ts"]
}
```

**angular.json (test section):**
```json
{
  "projects": {
    "portfolio-frontend": {
      "architect": {
        "test": {
          "builder": "@angular-devkit/build-angular:karma",
          "options": {
            "karmaConfig": "karma.conf.js"
          }
        }
      }
    }
  }
}
```

### CRITICAL: TypeScript Type Rules in Tests

**[REQUIRED] Use `undefined` for Optional Properties, NOT `null`**

```typescript
// [BAD] - Using null for optional properties
const formValue = {
  title: 'Test',
  endDate: null  // TypeScript error if endDate?: string
};

// [GOOD] - Use undefined for optional properties
const formValue = {
  title: 'Test',
  endDate: undefined  // Correct for endDate?: string
};

// [ALSO GOOD] - Omit optional properties entirely
const formValue = {
  title: 'Test'
  // endDate is omitted, which is fine for optional properties
};
```

**Rationale:**
- TypeScript optional properties (`property?: type`) can be `type | undefined`
- `null` is NOT the same as `undefined` in TypeScript
- Jasmine's `toHaveBeenCalledWith()` enforces strict type checking
- Using `null` for optional properties will cause compilation errors

### Frontend Test Pattern

```typescript
describe('Service', () => {
  let service: Service;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [Service, LoggerService]
    });
    service = TestBed.inject(Service);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should <behavior> when <condition>', () => {
    // Test implementation
  });

  afterEach(() => {
    httpMock.verify();
  });
});
```

---

## Test Naming Conventions

### Backend (Given-When-Then)

**Pattern:** `should_<expectedBehavior>_when_<condition>`

**Examples:**
```java
@Test
void should_returnProject_when_validId() { }

@Test
void should_throwNotFoundException_when_projectNotFound() { }

@Test
void should_updateProject_when_validData() { }
```

### Frontend (Behavior Description)

**Pattern:** `should <behavior> when <condition>`

**Examples:**
```typescript
it('should display project list when loaded', () => {});

it('should show error message when API fails', () => {});

it('should navigate to detail page when clicking project', () => {});
```

---

## Test Execution

### Backend Commands

```bash
# Unit tests only (fast, no Docker)
./gradlew test

# Integration tests (Testcontainers, Docker required)
./gradlew integrationTest

# All tests
./gradlew test integrationTest

# Coverage report
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html

# Coverage verification (fails if < 80%)
./gradlew jacocoTestCoverageVerification
```

### Frontend Commands

```bash
# Unit tests (watch mode)
npm test

# Single run (CI mode)
npm run test:ci

# Coverage report
npm run test:coverage
# Report: coverage/index.html
```

---

## Coverage Requirements

### Backend (JaCoCo)

**Thresholds:**
- **Global**: 80% minimum
- **Per package**: 70% minimum

**Exclusions:**
- Entities (JPA models)
- DTOs (Data Transfer Objects)
- Configurations (Spring configs)
- Exceptions (Custom exception classes)
- Mappers (MapStruct interfaces)

**Configuration:** See `build.gradle` lines 122-167

### Frontend (Karma/Istanbul)

**Thresholds:**
- **Statements**: 80%
- **Branches**: 75%
- **Functions**: 80%
- **Lines**: 80%

**Configuration:** See `karma.conf.js`

---

## Docker for Integration Tests

### Requirements

**[REQUIRED] Integration tests:**
- Must use Docker (Testcontainers)
- Full system tests require `docker-compose` environment
- Database, message brokers, external services → Docker

**[ALLOWED] Unit tests:**
- JUnit + Mockito can run without Docker
- Fast feedback loop for TDD
- No external dependencies needed

### Commands

```bash
# Unit tests only (no Docker)
./gradlew test

# Integration tests with Testcontainers (Docker required)
./gradlew integrationTest

# Full system test with docker-compose
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d
./gradlew test integrationTest
```

---

## Best Practices

### Test Independence

**[REQUIRED] Tests must:**
- Run independently of other tests
- Have no reliance on execution order
- Clean up resources after execution
- Have no shared state between tests

### Mock Usage

**[DO]:**
- Use lenient stubs for optional dependencies
- Spy on external dependencies when needed
- Verify interactions when behavior matters

**[DON'T]:**
- Over-mock internal logic
- Mock value objects or DTOs
- Create fragile tests tied to implementation details

### Test Data

**[DO]:**
- Use test builders or factories for complex objects
- Keep test data minimal and focused
- Use descriptive variable names

**[DON'T]:**
- Share test data across multiple tests
- Use production data in tests
- Create overly complex test setups

---

## Related Documentation

- [Code Quality](./code-quality.md) - Checkstyle, SpotBugs, JaCoCo configuration
- [Setup Guide](./setup.md) - Development environment setup
