package com.emmanuelgabe.portfolio.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void should_returnUpStatus_when_healthCalled() {
        // Act
        Health health = customHealthIndicator.health();

        // Assert
        assertNotNull(health, "Health should not be null");
        assertEquals(Status.UP, health.getStatus(), "Status should be UP");
    }

    @Test
    void should_containAppDetail_when_healthCalled() {
        // Act
        Health health = customHealthIndicator.health();

        // Assert
        assertNotNull(health.getDetails(), "Details should not be null");
        assertTrue(health.getDetails().containsKey("app"), "Details should contain 'app' key");
        assertEquals("Portfolio Backend", health.getDetails().get("app"), "App name should match");
    }

    @Test
    void should_containStatusDetail_when_healthCalled() {
        // Act
        Health health = customHealthIndicator.health();

        // Assert
        assertNotNull(health.getDetails(), "Details should not be null");
        assertTrue(health.getDetails().containsKey("status"), "Details should contain 'status' key");
        assertEquals("Application is running smoothly", health.getDetails().get("status"),
            "Status message should match");
    }

    @Test
    void should_notContainErrorDetails_when_healthCalled() {
        // Act
        Health health = customHealthIndicator.health();

        // Assert
        assertNotNull(health.getDetails(), "Details should not be null");
        assertFalse(health.getDetails().containsKey("error"), "Details should not contain 'error' key when healthy");
    }

    @Test
    void should_haveCorrectDetailsStructure_when_healthCalled() {
        // Act
        Health health = customHealthIndicator.health();

        // Assert
        assertNotNull(health.getDetails(), "Details should not be null");
        assertEquals(2, health.getDetails().size(), "Details should contain exactly 2 entries");
        assertTrue(health.getDetails().containsKey("app"), "Should contain app detail");
        assertTrue(health.getDetails().containsKey("status"), "Should contain status detail");
    }

    @Test
    void should_returnConsistentResults_when_calledMultipleTimes() {
        // Act
        Health health1 = customHealthIndicator.health();
        Health health2 = customHealthIndicator.health();

        // Assert
        assertEquals(health1.getStatus(), health2.getStatus(), "Status should be consistent");
        assertEquals(health1.getDetails().get("app"), health2.getDetails().get("app"),
            "App detail should be consistent");
        assertEquals(health1.getDetails().get("status"), health2.getDetails().get("status"),
            "Status detail should be consistent");
    }

    @Test
    void should_returnBuiltHealthObject_when_healthCalled() {
        // Act
        Health health = customHealthIndicator.health();

        // Assert
        assertNotNull(health, "Health should not be null");
        assertNotNull(health.getStatus(), "Status should not be null");
        assertNotNull(health.getDetails(), "Details should not be null");
        assertFalse(health.getDetails().isEmpty(), "Details should not be empty");
    }
}
