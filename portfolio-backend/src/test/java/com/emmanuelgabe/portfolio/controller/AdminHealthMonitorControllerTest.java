package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.health.HealthReportResponse;
import com.emmanuelgabe.portfolio.dto.health.ProposedActionResponse;
import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.emmanuelgabe.portfolio.healthmonitor.HealthAgentService;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorEventPublisher;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorMapper;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorMetrics;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorProperties;
import com.emmanuelgabe.portfolio.healthmonitor.HealthPreCheckService;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionLevel;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionType;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;
import com.emmanuelgabe.portfolio.healthmonitor.action.ReportSeverity;
import com.emmanuelgabe.portfolio.healthmonitor.executor.ActionExecutionResult;
import com.emmanuelgabe.portfolio.healthmonitor.executor.ActionExecutor;
import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import com.emmanuelgabe.portfolio.repository.HealthReportRepository;
import com.emmanuelgabe.portfolio.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminHealthMonitorControllerTest {

    @Mock
    private HealthAgentService agentService;

    @Mock
    private ObjectProvider<ActionExecutor> actionExecutorProvider;

    @Mock
    private ObjectProvider<HealthPreCheckService> preCheckServiceProvider;

    @Mock
    private HealthReportRepository reportRepository;

    @Mock
    private HealthProposedActionRepository actionRepository;

    @Mock
    private HealthMonitorMapper mapper;

    @Mock
    private HealthMonitorEventPublisher eventPublisher;

    @Mock
    private HealthMonitorMetrics metrics;

    @Mock
    private AuditService auditService;

    @Mock
    private ActionExecutor actionExecutor;

    @Mock
    private HealthPreCheckService preCheckService;

    private HealthMonitorProperties properties;
    private AdminHealthMonitorController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new HealthMonitorProperties(
                true, false, "0 0 * * * *", true, 30, 20,
                new HealthMonitorProperties.Docker("tcp://docker-socket-proxy:2375"),
                new HealthMonitorProperties.History(3, 5),
                new HealthMonitorProperties.Logs(100));
        controller = new AdminHealthMonitorController(
                agentService, actionExecutorProvider, preCheckServiceProvider,
                reportRepository, actionRepository, mapper, eventPublisher,
                metrics, properties, auditService);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    // ========== GET /reports ==========

    @Test
    void should_returnReports_when_listReportsCalled() throws Exception {
        // Arrange
        HealthReport report = sampleReport();
        Page<HealthReport> page = new PageImpl<>(List.of(report), Pageable.unpaged(), 1);
        when(reportRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        when(mapper.toResponse(report)).thenReturn(sampleReportResponse());

        // Act & Assert
        mockMvc.perform(get("/api/admin/health-monitor/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].severity", is("WARNING")));
    }

    @Test
    void should_capLimitAtHundred_when_listReportsCalledWithLargeLimit() throws Exception {
        // Arrange
        Page<HealthReport> page = new PageImpl<>(List.of(), Pageable.unpaged(), 0);
        when(reportRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        // Act
        mockMvc.perform(get("/api/admin/health-monitor/reports").param("limit", "9999"))
                .andExpect(status().isOk());

        // Assert
        verify(reportRepository).findAllByOrderByCreatedAtDesc(argThat(
                pageable -> pageable != null && pageable.getPageSize() == 100));
    }

    // ========== GET /reports/{id} ==========

    @Test
    void should_returnReport_when_getReportFound() throws Exception {
        // Arrange
        HealthReport report = sampleReport();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(mapper.toResponse(report)).thenReturn(sampleReportResponse());

        // Act & Assert
        mockMvc.perform(get("/api/admin/health-monitor/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void should_returnNotFound_when_getReportCalledWithUnknownId() throws Exception {
        // Arrange
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/admin/health-monitor/reports/999"))
                .andExpect(status().isNotFound());
    }

    // ========== POST /analyze-now ==========

    @Test
    void should_runAgentAndReturnReport_when_analyzeNowCalledWithBypassPrecheck() throws Exception {
        // Arrange: default properties has onDemandBypassPrecheck=true
        HealthReport report = sampleReport();
        when(agentService.analyze(HealthCycleType.ON_DEMAND)).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(sampleReportResponse());

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/analyze-now"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(agentService).analyze(HealthCycleType.ON_DEMAND);
        verify(eventPublisher).publishNewReport(any());
    }

    @Test
    void should_returnNoContent_when_precheckCleanAndBypassDisabled() throws Exception {
        // Arrange
        HealthMonitorProperties noBypass = new HealthMonitorProperties(
                true, false, "0 0 * * * *", false, 30, 20,
                new HealthMonitorProperties.Docker("tcp://docker-socket-proxy:2375"),
                new HealthMonitorProperties.History(3, 5),
                new HealthMonitorProperties.Logs(100));
        AdminHealthMonitorController c = new AdminHealthMonitorController(
                agentService, actionExecutorProvider, preCheckServiceProvider,
                reportRepository, actionRepository, mapper, eventPublisher,
                metrics, noBypass, auditService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(c)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        when(preCheckServiceProvider.getIfAvailable()).thenReturn(preCheckService);
        when(preCheckService.run())
                .thenReturn(new HealthPreCheckService.PreCheckResult(false, "healthy"));

        // Act & Assert
        mvc.perform(post("/api/admin/health-monitor/analyze-now"))
                .andExpect(status().isNoContent());

        verify(agentService, never()).analyze(any());
    }

    @Test
    void should_runAgent_when_precheckFlagsAnomalyAndBypassDisabled() throws Exception {
        // Arrange
        HealthMonitorProperties noBypass = new HealthMonitorProperties(
                true, false, "0 0 * * * *", false, 30, 20,
                new HealthMonitorProperties.Docker("tcp://docker-socket-proxy:2375"),
                new HealthMonitorProperties.History(3, 5),
                new HealthMonitorProperties.Logs(100));
        AdminHealthMonitorController c = new AdminHealthMonitorController(
                agentService, actionExecutorProvider, preCheckServiceProvider,
                reportRepository, actionRepository, mapper, eventPublisher,
                metrics, noBypass, auditService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(c)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        when(preCheckServiceProvider.getIfAvailable()).thenReturn(preCheckService);
        when(preCheckService.run())
                .thenReturn(new HealthPreCheckService.PreCheckResult(true, "dlq=5"));
        HealthReport report = sampleReport();
        when(agentService.analyze(HealthCycleType.ON_DEMAND)).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(sampleReportResponse());

        // Act & Assert
        mvc.perform(post("/api/admin/health-monitor/analyze-now"))
                .andExpect(status().isOk());

        verify(agentService).analyze(HealthCycleType.ON_DEMAND);
    }

    // ========== GET /actions/pending ==========

    @Test
    void should_returnPendingActions_when_listPendingCalled() throws Exception {
        // Arrange
        HealthProposedAction action = sampleAction(ActionLevel.ORANGE);
        when(actionRepository.findByStatusOrderByCreatedAtDesc(ActionStatus.PENDING))
                .thenReturn(List.of(action));
        when(mapper.toResponse(action)).thenReturn(sampleActionResponse());

        // Act & Assert
        mockMvc.perform(get("/api/admin/health-monitor/actions/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    // ========== POST /actions/{id}/approve ==========

    @Test
    void should_returnNotFound_when_approveCalledWithUnknownActionId() throws Exception {
        // Arrange
        when(actionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/999/approve")
                        .contentType("application/json")
                        .content("{\"reason\":\"ok\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_returnBadRequest_when_approveCalledOnRedActionWithoutReason() throws Exception {
        // Arrange
        HealthProposedAction redAction = sampleAction(ActionLevel.RED);
        when(actionRepository.findById(1L)).thenReturn(Optional.of(redAction));

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/1/approve")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(actionRepository, never())
                .tryApprove(anyLong(), any(), any(), anyString(), any());
    }

    @Test
    void should_executeApprovedAction_when_approveCalledOnOrangeAction() throws Exception {
        // Arrange
        HealthProposedAction action = sampleAction(ActionLevel.ORANGE);
        when(actionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(actionRepository.tryApprove(eq(1L), eq(ActionStatus.PENDING),
                eq(ActionStatus.APPROVED), anyString(), any(LocalDateTime.class))).thenReturn(1);
        when(actionExecutorProvider.getIfAvailable()).thenReturn(actionExecutor);
        when(actionExecutor.execute(action)).thenReturn(ActionExecutionResult.ok("flushed"));
        when(actionRepository.save(action)).thenReturn(action);
        when(mapper.toResponse(action)).thenReturn(sampleActionResponse());

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/1/approve")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        verify(actionExecutor).execute(action);
        verify(metrics).recordActionExecuted(ActionType.FLUSH_REDIS_CACHE, true);
        verify(eventPublisher).publishActionUpdated(any());
    }

    @Test
    void should_returnConflict_when_approveRaceLosesCompareAndSet() throws Exception {
        // Arrange
        HealthProposedAction action = sampleAction(ActionLevel.ORANGE);
        when(actionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(actionRepository.tryApprove(eq(1L), eq(ActionStatus.PENDING),
                eq(ActionStatus.APPROVED), anyString(), any(LocalDateTime.class))).thenReturn(0);

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/1/approve")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isConflict());

        verify(actionExecutor, never()).execute(any());
    }

    @Test
    void should_markFailed_when_approveAndExecutorUnavailable() throws Exception {
        // Arrange
        HealthProposedAction action = sampleAction(ActionLevel.ORANGE);
        when(actionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(actionRepository.tryApprove(eq(1L), eq(ActionStatus.PENDING),
                eq(ActionStatus.APPROVED), anyString(), any(LocalDateTime.class))).thenReturn(1);
        when(actionExecutorProvider.getIfAvailable()).thenReturn(null);
        when(actionRepository.save(action)).thenReturn(action);
        when(mapper.toResponse(action)).thenReturn(sampleActionResponse());

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/1/approve")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        verify(metrics).recordActionExecuted(eq(ActionType.FLUSH_REDIS_CACHE), eq(false));
    }

    // ========== POST /actions/{id}/reject ==========

    @Test
    void should_returnBadRequest_when_rejectCalledWithoutReason() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/1/reject")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(actionRepository, never()).findById(anyLong());
    }

    @Test
    void should_returnNotFound_when_rejectCalledWithUnknownActionId() throws Exception {
        // Arrange
        when(actionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/999/reject")
                        .contentType("application/json")
                        .content("{\"reason\":\"not needed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_returnConflict_when_rejectRaceLosesCompareAndSet() throws Exception {
        // Arrange
        HealthProposedAction action = sampleAction(ActionLevel.ORANGE);
        when(actionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(actionRepository.tryReject(eq(1L), eq(ActionStatus.PENDING),
                eq(ActionStatus.REJECTED), anyString(), any(LocalDateTime.class), anyString()))
                .thenReturn(0);

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/1/reject")
                        .contentType("application/json")
                        .content("{\"reason\":\"not needed\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void should_rejectAction_when_rejectCalledWithValidReason() throws Exception {
        // Arrange
        HealthProposedAction action = sampleAction(ActionLevel.ORANGE);
        action.setStatus(ActionStatus.REJECTED);
        when(actionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(actionRepository.tryReject(eq(1L), eq(ActionStatus.PENDING),
                eq(ActionStatus.REJECTED), anyString(), any(LocalDateTime.class), anyString()))
                .thenReturn(1);
        when(mapper.toResponse(action))
                .thenReturn(sampleActionResponse());

        // Act & Assert
        mockMvc.perform(post("/api/admin/health-monitor/actions/1/reject")
                        .contentType("application/json")
                        .content("{\"reason\":\"not needed\"}"))
                .andExpect(status().isOk());

        verify(metrics).recordActionRejected(ActionType.FLUSH_REDIS_CACHE);
        verify(eventPublisher).publishActionUpdated(any());
    }

    // ========== Helpers ==========

    private HealthReport sampleReport() {
        HealthReport r = new HealthReport();
        r.setId(1L);
        r.setCycleType(HealthCycleType.SCHEDULED);
        r.setSeverity(ReportSeverity.WARNING);
        r.setSummary("sample");
        r.setAnomaliesJson("[]");
        r.setToolCallsJson("[]");
        r.setDurationMs(100L);
        return r;
    }

    private HealthReportResponse sampleReportResponse() {
        return new HealthReportResponse(
                1L, HealthCycleType.SCHEDULED, ReportSeverity.WARNING,
                "sample", List.of(), List.of(), 100L, LocalDateTime.now());
    }

    private HealthProposedAction sampleAction(ActionLevel level) {
        HealthProposedAction a = new HealthProposedAction();
        a.setId(1L);
        a.setReportId(1L);
        a.setLevel(level);
        a.setActionType(level == ActionLevel.RED
                ? ActionType.RESTART_SERVICE : ActionType.FLUSH_REDIS_CACHE);
        a.setStatus(ActionStatus.PENDING);
        a.setJustification("reason");
        a.setParamsJson("{}");
        a.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return a;
    }

    private ProposedActionResponse sampleActionResponse() {
        return new ProposedActionResponse(
                1L, 1L, ActionLevel.ORANGE, ActionType.FLUSH_REDIS_CACHE,
                Map.of(), "reason", ActionStatus.PENDING,
                null, null, null, null, null,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now());
    }

    private static <T> T argThat(org.mockito.ArgumentMatcher<T> matcher) {
        return org.mockito.ArgumentMatchers.argThat(matcher);
    }
}
