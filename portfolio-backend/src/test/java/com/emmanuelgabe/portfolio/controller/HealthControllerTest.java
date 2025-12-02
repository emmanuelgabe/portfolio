package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.CompleteHealthResponse;
import com.emmanuelgabe.portfolio.dto.DatabaseHealthResponse;
import com.emmanuelgabe.portfolio.dto.HealthResponse;
import com.emmanuelgabe.portfolio.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for HealthController
 * Tests all REST endpoints for health checks
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthService healthService;

    @Test
    void should_returnOkStatus_when_pingCalled() throws Exception {
        // Arrange
        HealthResponse healthResponse = new HealthResponse(
            "ok",
            "Backend API is responding",
            System.currentTimeMillis()
        );
        when(healthService.ping()).thenReturn(healthResponse);

        // Act & Assert
        mockMvc.perform(get("/api/health/ping"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.message").value("Backend API is responding"))
            .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void should_return200_when_checkDatabaseCalledWithHealthyDatabase() throws Exception {
        // Arrange
        DatabaseHealthResponse dbResponse = new DatabaseHealthResponse();
        dbResponse.setStatus("ok");
        dbResponse.setMessage("Database connection is healthy");
        dbResponse.setDatabase("PostgreSQL");

        when(healthService.checkDatabase()).thenReturn(dbResponse);

        // Act & Assert
        mockMvc.perform(get("/api/health/db"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.message").value("Database connection is healthy"))
            .andExpect(jsonPath("$.database").value("PostgreSQL"));
    }

    @Test
    void should_return503_when_checkDatabaseCalledWithUnhealthyDatabase() throws Exception {
        // Arrange
        DatabaseHealthResponse dbResponse = new DatabaseHealthResponse();
        dbResponse.setStatus("error");
        dbResponse.setMessage("Database connection failed");
        dbResponse.setError("Connection timeout");

        when(healthService.checkDatabase()).thenReturn(dbResponse);

        // Act & Assert
        mockMvc.perform(get("/api/health/db"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").value("Database connection failed"))
            .andExpect(jsonPath("$.error").value("Connection timeout"));
    }

    @Test
    void should_return200_when_getStatusCalledWithHealthySystem() throws Exception {
        // Arrange
        Map<String, Object> checks = new HashMap<>();
        checks.put("api", Map.of("status", "ok", "message", "API is responding"));
        checks.put("database", Map.of("status", "ok", "message", "Database connection is healthy"));

        CompleteHealthResponse completeResponse = new CompleteHealthResponse(
            "healthy",
            checks,
            System.currentTimeMillis()
        );

        when(healthService.getCompleteStatus()).thenReturn(completeResponse);

        // Act & Assert
        mockMvc.perform(get("/api/health/status"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.checks.api.status").value("ok"))
            .andExpect(jsonPath("$.checks.database.status").value("ok"))
            .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void should_return503_when_getStatusCalledWithUnhealthySystem() throws Exception {
        // Arrange
        Map<String, Object> checks = new HashMap<>();
        checks.put("api", Map.of("status", "ok", "message", "API is responding"));
        checks.put("database", Map.of("status", "error", "message", "Database connection failed"));

        CompleteHealthResponse completeResponse = new CompleteHealthResponse(
            "unhealthy",
            checks,
            System.currentTimeMillis()
        );

        when(healthService.getCompleteStatus()).thenReturn(completeResponse);

        // Act & Assert
        mockMvc.perform(get("/api/health/status"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("unhealthy"))
            .andExpect(jsonPath("$.checks.api.status").value("ok"))
            .andExpect(jsonPath("$.checks.database.status").value("error"))
            .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void should_verifyContentStructure_when_pingCalled() throws Exception {
        // Arrange
        HealthResponse healthResponse = new HealthResponse(
            "ok",
            "Backend API is responding",
            1234567890L
        );
        when(healthService.ping()).thenReturn(healthResponse);

        // Act & Assert
        mockMvc.perform(get("/api/health/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_verifyEndpointPath_when_checkDatabaseCalled() throws Exception {
        // Arrange
        DatabaseHealthResponse dbResponse = new DatabaseHealthResponse();
        dbResponse.setStatus("ok");
        dbResponse.setMessage("Database connection is healthy");

        when(healthService.checkDatabase()).thenReturn(dbResponse);

        // Act & Assert - verify correct endpoint path
        mockMvc.perform(get("/api/health/db"))
            .andExpect(status().isOk());
    }

    @Test
    void should_verifyEndpointPath_when_getStatusCalled() throws Exception {
        // Arrange
        Map<String, Object> checks = new HashMap<>();
        checks.put("api", Map.of("status", "ok"));
        checks.put("database", Map.of("status", "ok"));

        CompleteHealthResponse completeResponse = new CompleteHealthResponse(
            "healthy",
            checks,
            System.currentTimeMillis()
        );

        when(healthService.getCompleteStatus()).thenReturn(completeResponse);

        // Act & Assert - verify correct endpoint path
        mockMvc.perform(get("/api/health/status"))
            .andExpect(status().isOk());
    }
}