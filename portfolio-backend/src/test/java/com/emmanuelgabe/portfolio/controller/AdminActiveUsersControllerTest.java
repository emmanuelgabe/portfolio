package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.repository.DailyStatsRepository;
import com.emmanuelgabe.portfolio.service.VisitorTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminActiveUsersController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminActiveUsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitorTrackingService visitorTrackingService;

    @MockBean
    private DailyStatsRepository dailyStatsRepository;

    // ========== Count Endpoint Tests ==========

    @Test
    void should_returnActiveUsersCount_when_getCountCalled() throws Exception {
        // Arrange
        when(visitorTrackingService.getActiveVisitorsCount()).thenReturn(42);

        // Act & Assert
        mockMvc.perform(get("/api/admin/visitors/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(42))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_returnZero_when_noActiveVisitors() throws Exception {
        // Arrange
        when(visitorTrackingService.getActiveVisitorsCount()).thenReturn(0);

        // Act & Assert
        mockMvc.perform(get("/api/admin/visitors/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // ========== SSE Stream Tests ==========

    @Test
    void should_returnSseStream_when_streamEndpointCalled() throws Exception {
        // Arrange
        when(visitorTrackingService.getActiveVisitorsCount()).thenReturn(10);

        // Act & Assert
        mockMvc.perform(get("/api/admin/visitors/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));
    }

    // ========== Daily Data Tests ==========

    @Test
    void should_return7DailyDataPoints_when_getDailyCalled() throws Exception {
        // Arrange
        when(dailyStatsRepository.findByStatsDateBetweenOrderByStatsDateDesc(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/admin/visitors/daily"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].date").exists())
                .andExpect(jsonPath("$[0].count").exists());
    }
}
