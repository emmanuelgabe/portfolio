# Backend Testing Guide

**Document Type:** Technical Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active

---

## Table of Contents

1. [Overview](#1-overview)
2. [Test Structure](#2-test-structure)
3. [Test Implementation](#3-test-implementation)
4. [Configuration](#4-configuration)

---

## 1. Overview

Backend tests use JUnit 5 and Mockito to verify business logic, REST endpoints, and health indicators.

**Test Coverage:**
- HealthService: 9 tests
- HealthController: 8 tests
- CustomHealthIndicator: 7 tests

---

## 2. Test Structure

```
portfolio-backend/src/test/java/com/emmanuelgabe/portfolio/
├── controller/
│   └── HealthControllerTest.java
├── service/
│   └── HealthServiceTest.java
└── health/
    └── CustomHealthIndicatorTest.java
```

---

## 3. Test Implementation

### 3.1 HealthServiceTest

**Location:** `portfolio-backend/src/test/java/com/emmanuelgabe/portfolio/service/HealthServiceTest.java`

**Purpose:** Verifies business logic for health check operations

**Test Pattern Example:**
```java
@ExtendWith(MockitoExtension.class)
class HealthServiceTest {
    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @InjectMocks
    private HealthService healthService;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    void testPing_ShouldReturnOkStatus() {
        // Arrange
        // Act
        HealthResponse response = healthService.ping();
        // Assert
        assertNotNull(response);
        assertEquals("ok", response.getStatus());
        assertTrue(response.getTimestamp() > 0);
    }
}
```

**Key Test Cases:**
- Ping response validation
- Database connection success/failure
- Exception handling
- Overall health check (healthy/unhealthy)
- Resource management (connection closing)

### 3.2 HealthControllerTest

**Location:** `portfolio-backend/src/test/java/com/emmanuelgabe/portfolio/controller/HealthControllerTest.java`

**Purpose:** Validates REST endpoint behavior and HTTP responses

**Configuration:**
```java
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class HealthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthService healthService;

    @Test
    void testPing_ShouldReturnOkStatus() throws Exception {
        HealthResponse healthResponse = new HealthResponse(
            "ok", "Backend API is responding", System.currentTimeMillis()
        );
        when(healthService.ping()).thenReturn(healthResponse);

        mockMvc.perform(get("/api/health/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }
}
```

**Key Test Cases:**
- Ping endpoint (200 OK)
- Database check success (200 OK)
- Database check failure (503 Service Unavailable)
- Complete status checks

### 3.3 CustomHealthIndicatorTest

**Location:** `portfolio-backend/src/test/java/com/emmanuelgabe/portfolio/health/CustomHealthIndicatorTest.java`

**Purpose:** Validates Spring Boot Actuator health indicator implementation

**Key Test Cases:**
- Health indicator status (UP)
- Application details present
- Response structure validation
- Consistency across multiple calls

---

## 4. Configuration

### 4.1 Security Bypass for Tests

Controller tests require security filters to be disabled:

```java
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
```

### 4.2 Lenient Mocking

Use lenient stubs for optional dependencies:

```java
@BeforeEach
void setUp() throws SQLException {
    lenient().when(dataSource.getConnection()).thenReturn(connection);
}
```

### 4.3 Required Dependencies

Ensure `build.gradle` includes:

```gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.security:spring-security-test'
```

---

## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Technical Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active
