package com.emmanuelgabe.portfolio.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitPolicyTest {

    // ========== Validation Tests ==========

    @Test
    void should_createPolicy_when_allFieldsValid() {
        // Arrange & Act
        RateLimitPolicy policy = new RateLimitPolicy("chat", 10, Duration.ofHours(1));

        // Assert
        assertThat(policy.bucketName()).isEqualTo("chat");
        assertThat(policy.maxRequests()).isEqualTo(10);
        assertThat(policy.window()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void should_throwException_when_bucketNameIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new RateLimitPolicy(null, 10, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bucketName");
    }

    @Test
    void should_throwException_when_bucketNameIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> new RateLimitPolicy("   ", 10, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bucketName");
    }

    @Test
    void should_throwException_when_maxRequestsIsZero() {
        // Act & Assert
        assertThatThrownBy(() -> new RateLimitPolicy("chat", 0, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRequests");
    }

    @Test
    void should_throwException_when_maxRequestsIsNegative() {
        // Act & Assert
        assertThatThrownBy(() -> new RateLimitPolicy("chat", -5, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRequests");
    }

    @Test
    void should_throwException_when_windowIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new RateLimitPolicy("chat", 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window");
    }

    @Test
    void should_throwException_when_windowIsZero() {
        // Act & Assert
        assertThatThrownBy(() -> new RateLimitPolicy("chat", 10, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window");
    }

    @Test
    void should_throwException_when_windowIsNegative() {
        // Act & Assert
        assertThatThrownBy(() -> new RateLimitPolicy("chat", 10, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window");
    }

    // ========== Factory Method Tests ==========

    @Test
    void should_createHourlyPolicy_when_perHourFactoryCalled() {
        // Arrange & Act
        RateLimitPolicy policy = RateLimitPolicy.perHour("chat", 10);

        // Assert
        assertThat(policy.bucketName()).isEqualTo("chat");
        assertThat(policy.maxRequests()).isEqualTo(10);
        assertThat(policy.window()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void should_enforceValidationInFactory_when_perHourCalledWithInvalidArgs() {
        // Act & Assert
        assertThatThrownBy(() -> RateLimitPolicy.perHour("", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
