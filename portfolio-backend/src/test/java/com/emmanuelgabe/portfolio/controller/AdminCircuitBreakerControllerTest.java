package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCircuitBreakerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminCircuitBreakerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CircuitBreakerRegistry circuitBreakerRegistry;

    // ========== Get All Circuit Breakers Tests ==========

    @Test
    void should_returnAllCircuitBreakers_when_getAllCalled() throws Exception {
        // Arrange
        CircuitBreaker mockCb = createMockCircuitBreaker("emailService", CircuitBreaker.State.CLOSED);
        when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of(mockCb));

        // Act & Assert
        mockMvc.perform(get("/api/admin/circuit-breakers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("emailService"))
                .andExpect(jsonPath("$[0].state").value("CLOSED"))
                .andExpect(jsonPath("$[0].timestamp").exists());
    }

    @Test
    void should_returnEmptyList_when_noCircuitBreakersConfigured() throws Exception {
        // Arrange
        when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Collections.emptySet());

        // Act & Assert
        mockMvc.perform(get("/api/admin/circuit-breakers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ========== Get Single Circuit Breaker Tests ==========

    @Test
    void should_returnCircuitBreaker_when_getByNameCalledWithValidName() throws Exception {
        // Arrange
        CircuitBreaker mockCb = createMockCircuitBreaker("emailService", CircuitBreaker.State.OPEN);
        when(circuitBreakerRegistry.find("emailService")).thenReturn(Optional.of(mockCb));

        // Act & Assert
        mockMvc.perform(get("/api/admin/circuit-breakers/emailService"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("emailService"))
                .andExpect(jsonPath("$.state").value("OPEN"));
    }

    @Test
    void should_returnNotFound_when_circuitBreakerNotExists() throws Exception {
        // Arrange
        when(circuitBreakerRegistry.find("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/admin/circuit-breakers/unknown"))
                .andExpect(status().isNotFound());
    }

    // ========== Helper Methods ==========

    private CircuitBreaker createMockCircuitBreaker(String name, CircuitBreaker.State state) {
        CircuitBreaker mockCb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics mockMetrics = mock(CircuitBreaker.Metrics.class);

        when(mockCb.getName()).thenReturn(name);
        when(mockCb.getState()).thenReturn(state);
        when(mockCb.getMetrics()).thenReturn(mockMetrics);
        when(mockMetrics.getNumberOfFailedCalls()).thenReturn(2);
        when(mockMetrics.getNumberOfSuccessfulCalls()).thenReturn(10);
        when(mockMetrics.getNumberOfBufferedCalls()).thenReturn(12);
        when(mockMetrics.getFailureRate()).thenReturn(16.67f);
        when(mockMetrics.getNumberOfNotPermittedCalls()).thenReturn(0L);

        return mockCb;
    }
}
