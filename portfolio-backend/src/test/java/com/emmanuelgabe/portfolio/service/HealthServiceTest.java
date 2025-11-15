package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CompleteHealthResponse;
import com.emmanuelgabe.portfolio.dto.DatabaseHealthResponse;
import com.emmanuelgabe.portfolio.dto.HealthResponse;
import com.emmanuelgabe.portfolio.service.impl.HealthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HealthService
 * Tests all health check operations and database connectivity
 */
@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @InjectMocks
    private HealthServiceImpl healthService;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    void testPing_ShouldReturnOkStatus() {
        // When
        HealthResponse response = healthService.ping();

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("ok", response.getStatus(), "Status should be 'ok'");
        assertEquals("Backend API is responding", response.getMessage(), "Message should match");
        assertTrue(response.getTimestamp() > 0, "Timestamp should be set");
    }

    @Test
    void testCheckDatabase_WhenConnectionIsValid_ShouldReturnOkStatus() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/portfolio");

        // When
        DatabaseHealthResponse response = healthService.checkDatabase();

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("ok", response.getStatus(), "Status should be 'ok'");
        assertEquals("Database connection is healthy", response.getMessage(), "Message should match");
        assertEquals("PostgreSQL", response.getDatabase(), "Database name should be PostgreSQL");
        assertEquals("jdbc:postgresql://localhost:5432/portfolio", response.getUrl(), "URL should match");
        assertNull(response.getError(), "Error should be null for successful connection");

        verify(connection).isValid(5);
        verify(connection, times(2)).getMetaData();
    }

    @Test
    void testCheckDatabase_WhenConnectionIsNotValid_ShouldReturnErrorStatus() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(false);

        // When
        DatabaseHealthResponse response = healthService.checkDatabase();

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("error", response.getStatus(), "Status should be 'error'");
        assertEquals("Database connection is not valid", response.getMessage(), "Message should match");
        assertNull(response.getDatabase(), "Database should be null");
        assertNull(response.getError(), "Error should be null for invalid connection");

        verify(connection).isValid(5);
        verify(connection, never()).getMetaData();
    }

    @Test
    void testCheckDatabase_WhenExceptionThrown_ShouldReturnErrorStatus() throws SQLException {
        // Given
        String errorMessage = "Connection failed";
        when(dataSource.getConnection()).thenThrow(new SQLException(errorMessage));

        // When
        DatabaseHealthResponse response = healthService.checkDatabase();

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("error", response.getStatus(), "Status should be 'error'");
        assertEquals("Database connection failed", response.getMessage(), "Message should match");
        assertEquals(errorMessage, response.getError(), "Error message should match");
        assertNull(response.getDatabase(), "Database should be null");
    }

    @Test
    void testGetCompleteStatus_WhenAllHealthy_ShouldReturnHealthyStatus() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/portfolio");

        // When
        CompleteHealthResponse response = healthService.getCompleteStatus();

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("healthy", response.getStatus(), "Overall status should be 'healthy'");
        assertNotNull(response.getChecks(), "Checks should not be null");
        assertTrue(response.getChecks().containsKey("api"), "Should contain API check");
        assertTrue(response.getChecks().containsKey("database"), "Should contain database check");
        assertTrue(response.getTimestamp() > 0, "Timestamp should be set");
    }

    @Test
    void testGetCompleteStatus_WhenDatabaseUnhealthy_ShouldReturnUnhealthyStatus() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(false);

        // When
        CompleteHealthResponse response = healthService.getCompleteStatus();

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("unhealthy", response.getStatus(), "Overall status should be 'unhealthy'");
        assertNotNull(response.getChecks(), "Checks should not be null");
        assertTrue(response.getChecks().containsKey("api"), "Should contain API check");
        assertTrue(response.getChecks().containsKey("database"), "Should contain database check");
    }

    @Test
    void testIsDatabaseHealthy_WhenHealthy_ShouldReturnTrue() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/portfolio");

        // When
        boolean isHealthy = healthService.isDatabaseHealthy();

        // Then
        assertTrue(isHealthy, "Database should be healthy");
    }

    @Test
    void testIsDatabaseHealthy_WhenUnhealthy_ShouldReturnFalse() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(false);

        // When
        boolean isHealthy = healthService.isDatabaseHealthy();

        // Then
        assertFalse(isHealthy, "Database should not be healthy");
    }

    @Test
    void testCheckDatabase_ShouldCloseConnection() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/portfolio");

        // When
        healthService.checkDatabase();

        // Then
        verify(connection).close();
    }
}