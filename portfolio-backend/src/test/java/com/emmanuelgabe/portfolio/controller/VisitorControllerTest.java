package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.service.VisitorTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VisitorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class VisitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitorTrackingService visitorTrackingService;

    // ========== Heartbeat Tests ==========

    @Test
    void should_returnOk_when_heartbeatCalledWithValidSessionId() throws Exception {
        // Arrange
        String sessionId = "test-session-123";

        // Act & Assert
        mockMvc.perform(post("/api/visitors/heartbeat")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk());

        verify(visitorTrackingService).registerHeartbeat(sessionId);
    }

    @Test
    void should_returnBadRequest_when_heartbeatCalledWithoutSessionId() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/visitors/heartbeat"))
                .andExpect(status().isBadRequest());

        verify(visitorTrackingService, never()).registerHeartbeat(anyString());
    }

    @Test
    void should_acceptUuidSessionId_when_heartbeatCalled() throws Exception {
        // Arrange
        String sessionId = "550e8400-e29b-41d4-a716-446655440000";

        // Act & Assert
        mockMvc.perform(post("/api/visitors/heartbeat")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk());

        verify(visitorTrackingService).registerHeartbeat(sessionId);
    }
}
