package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.ContactHistoryResponse;
import com.emmanuelgabe.portfolio.dto.ContactStatsResponse;
import com.emmanuelgabe.portfolio.moderation.ContactCategory;
import com.emmanuelgabe.portfolio.moderation.ModerationVerdict;
import com.emmanuelgabe.portfolio.service.ContactService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminContactController.
 * Tests all admin contact endpoints.
 */
@WebMvcTest(AdminContactController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactService contactService;

    // ========== Stats Endpoint Tests ==========

    @Test
    void should_returnStats_when_getStatsCalled() throws Exception {
        // Arrange
        ContactStatsResponse stats = new ContactStatsResponse(
                25L,
                Map.of("MISSION", 10L, "SPAM", 5L, "QUESTION", 10L),
                Map.of("APPROVED", 20L, "REJECTED", 5L),
                Map.of("2026-04-06", 12L, "2026-04-07", 13L)
        );
        when(contactService.getContactStats(any(LocalDateTime.class))).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/contacts/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(25)))
                .andExpect(jsonPath("$.categoryCounts.MISSION", is(10)))
                .andExpect(jsonPath("$.verdictCounts.APPROVED", is(20)))
                .andExpect(jsonPath("$.dailyActivity").isMap());
    }

    @Test
    void should_useCustomDays_when_daysParameterProvided() throws Exception {
        // Arrange
        ContactStatsResponse stats = new ContactStatsResponse(0L, Map.of(), Map.of(), Map.of());
        when(contactService.getContactStats(any(LocalDateTime.class))).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/contacts/stats").param("days", "7"))
                .andExpect(status().isOk());

        verify(contactService).getContactStats(any(LocalDateTime.class));
    }

    // ========== History Endpoint Tests ==========

    @Test
    void should_returnHistory_when_getHistoryCalled() throws Exception {
        // Arrange
        ContactHistoryResponse entry = new ContactHistoryResponse(
                1L, "John Doe", "Project Inquiry", "I want to collaborate",
                ContactCategory.MISSION, ModerationVerdict.APPROVED,
                "Legitimate inquiry", 0.95, LocalDateTime.now()
        );
        Page<ContactHistoryResponse> page = new PageImpl<>(List.of(entry));
        when(contactService.getContactHistory(isNull(), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/contacts/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("John Doe")))
                .andExpect(jsonPath("$.content[0].category", is("MISSION")));
    }

    @Test
    void should_filterByCategory_when_categoryParameterProvided() throws Exception {
        // Arrange
        Page<ContactHistoryResponse> page = new PageImpl<>(List.of());
        when(contactService.getContactHistory(eq(ContactCategory.SPAM), any(Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/contacts/history").param("category", "SPAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(contactService).getContactHistory(eq(ContactCategory.SPAM), any(Pageable.class));
    }

    // ========== Categories Endpoint Tests ==========

    @Test
    void should_returnAllCategories_when_getCategoriesCalled() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/contacts/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(ContactCategory.values().length)))
                .andExpect(jsonPath("$[0]", is("MISSION")));
    }
}
