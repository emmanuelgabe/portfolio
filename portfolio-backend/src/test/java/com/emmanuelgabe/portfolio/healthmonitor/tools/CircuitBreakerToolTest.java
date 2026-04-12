package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerToolTest {

    @Mock
    private CircuitBreakerRegistry registry;

    private HealthToolCallTracker tracker;
    private CircuitBreakerTool tool;

    @BeforeEach
    void setUp() {
        tracker = new HealthToolCallTracker();
        tool = new CircuitBreakerTool(registry, tracker);
    }

    // ========== Empty registry ==========

    @Test
    void should_returnEmptyList_when_checkCircuitBreakersCalledWithNoRegisteredBreakers() {
        // Arrange
        when(registry.getAllCircuitBreakers()).thenReturn(Collections.emptySet());

        // Act
        List<Map<String, Object>> result = tool.checkCircuitBreakers();

        // Assert
        assertThat(result).isEmpty();
        assertThat(tracker.getCalls()).containsExactly("checkCircuitBreakers");
    }

    // ========== Populated registry ==========

    @Test
    void should_returnBreakerStatesMap_when_checkCircuitBreakersCalledAndBreakersExist() {
        // Arrange
        CircuitBreaker breaker = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        when(breaker.getName()).thenReturn("gemini-api");
        when(breaker.getState()).thenReturn(CircuitBreaker.State.OPEN);
        when(breaker.getMetrics()).thenReturn(metrics);
        lenient().when(metrics.getFailureRate()).thenReturn(75.5f);
        lenient().when(metrics.getNumberOfBufferedCalls()).thenReturn(20);
        lenient().when(metrics.getNumberOfFailedCalls()).thenReturn(15);
        lenient().when(metrics.getNumberOfSuccessfulCalls()).thenReturn(5);
        lenient().when(metrics.getNumberOfNotPermittedCalls()).thenReturn(42L);

        when(registry.getAllCircuitBreakers()).thenReturn(Set.of(breaker));

        // Act
        List<Map<String, Object>> result = tool.checkCircuitBreakers();

        // Assert
        assertThat(result).hasSize(1);
        Map<String, Object> first = result.get(0);
        assertThat(first).containsEntry("name", "gemini-api");
        assertThat(first).containsEntry("state", "OPEN");
        assertThat(first).containsEntry("failureRate", 75.5f);
        assertThat(first).containsEntry("bufferedCalls", 20);
        assertThat(first).containsEntry("failedCalls", 15);
        assertThat(first).containsEntry("successCalls", 5);
        assertThat(first).containsEntry("notPermittedCalls", 42L);
    }

    @Test
    void should_returnMultipleBreakers_when_registryContainsSeveralBreakers() {
        // Arrange
        CircuitBreaker cb1 = buildBreaker("gemini-api", CircuitBreaker.State.CLOSED);
        CircuitBreaker cb2 = buildBreaker("elasticsearch", CircuitBreaker.State.HALF_OPEN);
        Set<CircuitBreaker> breakers = new LinkedHashSet<>();
        breakers.add(cb1);
        breakers.add(cb2);
        when(registry.getAllCircuitBreakers()).thenReturn(breakers);

        // Act
        List<Map<String, Object>> result = tool.checkCircuitBreakers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(m -> m.get("name"))
                .containsExactlyInAnyOrder("gemini-api", "elasticsearch");
    }

    private CircuitBreaker buildBreaker(String name, CircuitBreaker.State state) {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        when(cb.getName()).thenReturn(name);
        when(cb.getState()).thenReturn(state);
        when(cb.getMetrics()).thenReturn(metrics);
        lenient().when(metrics.getFailureRate()).thenReturn(0.0f);
        lenient().when(metrics.getNumberOfBufferedCalls()).thenReturn(0);
        lenient().when(metrics.getNumberOfFailedCalls()).thenReturn(0);
        lenient().when(metrics.getNumberOfSuccessfulCalls()).thenReturn(0);
        lenient().when(metrics.getNumberOfNotPermittedCalls()).thenReturn(0L);
        return cb;
    }
}
