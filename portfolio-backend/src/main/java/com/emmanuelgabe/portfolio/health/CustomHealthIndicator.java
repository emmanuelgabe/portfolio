package com.emmanuelgabe.portfolio.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator to check application-specific health
 * This will be automatically picked up by Spring Boot Actuator
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            boolean isHealthy = performHealthCheck();

            if (isHealthy) {
                return Health.up()
                        .withDetail("app", "Portfolio Backend")
                        .withDetail("status", "Application is running smoothly")
                        .build();
            } else {
                return Health.down()
                        .withDetail("app", "Portfolio Backend")
                        .withDetail("error", "Health check failed")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean performHealthCheck() {
        return true;
    }
}
