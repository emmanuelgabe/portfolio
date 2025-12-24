package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogFilter;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogResponse;
import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.emmanuelgabe.portfolio.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminAuditController.
 * Tests all admin audit endpoints.
 */
@WebMvcTest(AdminAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    private AuditLogResponse testAuditLogResponse;
    private AuditStatsResponse testStatsResponse;

    @BeforeEach
    void setUp() {
        testAuditLogResponse = new AuditLogResponse();
        testAuditLogResponse.setId(1L);
        testAuditLogResponse.setAction(AuditAction.CREATE);
        testAuditLogResponse.setEntityType("Project");
        testAuditLogResponse.setEntityId(1L);
        testAuditLogResponse.setEntityName("Test Project");
        testAuditLogResponse.setUsername("admin");
        testAuditLogResponse.setSuccess(true);
        testAuditLogResponse.setCreatedAt(LocalDateTime.now());

        testStatsResponse = new AuditStatsResponse();
        testStatsResponse.setTotalCount(100L);
        testStatsResponse.setFailedActionsCount(5L);
        testStatsResponse.setActionCounts(Map.of("CREATE", 50L, "UPDATE", 30L, "DELETE", 20L));
        testStatsResponse.setEntityTypeCounts(Map.of("Project", 40L, "Article", 30L));
        testStatsResponse.setDailyActivity(Map.of("2024-01-15", 10L, "2024-01-16", 15L));
        testStatsResponse.setUserActivity(Map.of("admin", 80L, "user1", 20L));
    }

    // ========== Get Audit Logs Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndPageOfLogs_when_getAuditLogs() throws Exception {
        // Arrange
        Page<AuditLogResponse> page = new PageImpl<>(
                List.of(testAuditLogResponse),
                Pageable.unpaged(),
                1
        );
        when(auditService.getAuditLogs(any(AuditLogFilter.class), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].action", is("CREATE")))
                .andExpect(jsonPath("$.content[0].entityType", is("Project")))
                .andExpect(jsonPath("$.content[0].username", is("admin")));

        verify(auditService).getAuditLogs(any(AuditLogFilter.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndFilteredLogs_when_getAuditLogsWithFilters() throws Exception {
        // Arrange
        Page<AuditLogResponse> page = new PageImpl<>(
                List.of(testAuditLogResponse),
                Pageable.unpaged(),
                1
        );
        when(auditService.getAuditLogs(any(AuditLogFilter.class), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit")
                        .param("action", "CREATE")
                        .param("entityType", "Project")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(auditService).getAuditLogs(any(AuditLogFilter.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndEmptyPage_when_getAuditLogsWithNoResults() throws Exception {
        // Arrange
        Page<AuditLogResponse> emptyPage = new PageImpl<>(List.of(), Pageable.unpaged(), 0);
        when(auditService.getAuditLogs(any(AuditLogFilter.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // ========== Get Entity History Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndHistory_when_getEntityHistory() throws Exception {
        // Arrange
        when(auditService.getEntityHistory("Project", 1L)).thenReturn(List.of(testAuditLogResponse));

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/entity/Project/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].entityType", is("Project")))
                .andExpect(jsonPath("$[0].entityId", is(1)));

        verify(auditService).getEntityHistory("Project", 1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndEmptyList_when_getEntityHistoryWithNoHistory() throws Exception {
        // Arrange
        when(auditService.getEntityHistory("Project", 999L)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/entity/Project/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ========== Get Stats Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndStats_when_getStats() throws Exception {
        // Arrange
        when(auditService.getStats(any(LocalDateTime.class))).thenReturn(testStatsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount", is(100)))
                .andExpect(jsonPath("$.failedActionsCount", is(5)))
                .andExpect(jsonPath("$.actionCounts.CREATE", is(50)))
                .andExpect(jsonPath("$.entityTypeCounts.Project", is(40)));

        verify(auditService).getStats(any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndStats_when_getStatsWithCustomDays() throws Exception {
        // Arrange
        when(auditService.getStats(any(LocalDateTime.class))).thenReturn(testStatsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/stats")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(100)));

        verify(auditService).getStats(any(LocalDateTime.class));
    }

    // ========== Export CSV Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndCsv_when_exportToCsv() throws Exception {
        // Arrange
        byte[] csvContent = "ID,Timestamp,Action\n1,2024-01-15,CREATE".getBytes(StandardCharsets.UTF_8);
        when(auditService.exportToCsv(any(AuditLogFilter.class))).thenReturn(csvContent);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/export/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=audit-logs.csv"));

        verify(auditService).exportToCsv(any(AuditLogFilter.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndFilteredCsv_when_exportToCsvWithFilters() throws Exception {
        // Arrange
        byte[] csvContent = "ID,Timestamp,Action\n1,2024-01-15,CREATE".getBytes(StandardCharsets.UTF_8);
        when(auditService.exportToCsv(any(AuditLogFilter.class))).thenReturn(csvContent);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/export/csv")
                        .param("action", "CREATE")
                        .param("entityType", "Project"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        verify(auditService).exportToCsv(any(AuditLogFilter.class));
    }

    // ========== Export JSON Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndJson_when_exportToJson() throws Exception {
        // Arrange
        byte[] jsonContent = "[{\"id\":1,\"action\":\"CREATE\"}]".getBytes(StandardCharsets.UTF_8);
        when(auditService.exportToJson(any(AuditLogFilter.class))).thenReturn(jsonContent);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/export/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Disposition", "attachment; filename=audit-logs.json"));

        verify(auditService).exportToJson(any(AuditLogFilter.class));
    }

    // ========== Get Actions Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndAllActions_when_getActions() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/actions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("CREATE"))
                .andExpect(jsonPath("$[1]").value("UPDATE"))
                .andExpect(jsonPath("$[2]").value("DELETE"));
    }

    // ========== Get Entity Types Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndEntityTypes_when_getEntityTypes() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/entity-types"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0]").value("Project"))
                .andExpect(jsonPath("$[1]").value("Article"));
    }
}
