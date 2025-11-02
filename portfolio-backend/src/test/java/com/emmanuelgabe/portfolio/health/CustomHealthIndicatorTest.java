package com.emmanuelgabe.portfolio.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomHealthIndicator
 * Tests Spring Boot Actuator health indicator functionality
 */
class CustomHealthIndicatorTest {

    private CustomHealthIndicator customHealthIndicator;

    @BeforeEach
    void setUp() {
        customHealthIndicator = new CustomHealthIndicator();
    }

    @Test
    void testHealth_ShouldReturnUpStatus() {
        // When
        Health health = customHealthIndicator.health();

        // Then
        assertNotNull(health, "Health should not be null");
        assertEquals(Status.UP, health.getStatus(), "Status should be UP");
    }

    @Test
    void testHealth_ShouldContainAppDetail() {
        // When
        Health health = customHealthIndicator.health();

        // Then
        assertNotNull(health.getDetails(), "Details should not be null");
        assertTrue(health.getDetails().containsKey("app"), "Details should contain 'app' key");
        assertEquals("Portfolio Backend", health.getDetails().get("app"), "App name should match");
    }

    @Test
    void testHealth_ShouldContainStatusDetail() {
        // When
        Health health = customHealthIndicator.health();

        // Then
        assertNotNull(health.getDetails(), "Details should not be null");
        assertTrue(health.getDetails().containsKey("status"), "Details should contain 'status' key");
        assertEquals("Application is running smoothly", health.getDetails().get("status"),
            "Status message should match");
    }

    @Test
    void testHealth_ShouldNotContainErrorDetails() {
        // When
        Health health = customHealthIndicator.health();

        // Then
        assertNotNull(health.getDetails(), "Details should not be null");
        assertFalse(health.getDetails().containsKey("error"), "Details should not contain 'error' key when healthy");
    }

    @Test
    void testHealth_VerifyDetailsStructure() {
        // When
        Health health = customHealthIndicator.health();

        // Then
        assertNotNull(health.getDetails(), "Details should not be null");
        assertEquals(2, health.getDetails().size(), "Details should contain exactly 2 entries");
        assertTrue(health.getDetails().containsKey("app"), "Should contain app detail");
        assertTrue(health.getDetails().containsKey("status"), "Should contain status detail");
    }

    @Test
    void testHealth_MultipleCallsReturnConsistentResults() {
        // When
        Health health1 = customHealthIndicator.health();
        Health health2 = customHealthIndicator.health();

        // Then
        assertEquals(health1.getStatus(), health2.getStatus(), "Status should be consistent");
        assertEquals(health1.getDetails().get("app"), health2.getDetails().get("app"),
            "App detail should be consistent");
        assertEquals(health1.getDetails().get("status"), health2.getDetails().get("status"),
            "Status detail should be consistent");
    }

    @Test
    void testHealth_ReturnsBuiltHealthObject() {
        // When
        Health health = customHealthIndicator.health();

        // Then
        assertNotNull(health, "Health should not be null");
        assertNotNull(health.getStatus(), "Status should not be null");
        assertNotNull(health.getDetails(), "Details should not be null");
        assertFalse(health.getDetails().isEmpty(), "Details should not be empty");
    }
}
