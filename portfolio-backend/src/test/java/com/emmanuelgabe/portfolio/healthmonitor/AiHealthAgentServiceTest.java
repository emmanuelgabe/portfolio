package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionLevel;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionType;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthReportDraft;
import com.emmanuelgabe.portfolio.healthmonitor.action.ProposedActionDraft;
import com.emmanuelgabe.portfolio.healthmonitor.action.ReportSeverity;
import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import com.emmanuelgabe.portfolio.repository.HealthReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiHealthAgentServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private HealthToolFactory toolFactory;

    @Mock
    private HealthPromptBuilder promptBuilder;

    @Mock
    private HealthReportRepository reportRepository;

    @Mock
    private HealthProposedActionRepository actionRepository;

    @Mock
    private HealthMonitorMetrics metrics;

    @Mock
    private HealthAlertService alertService;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private HealthMonitorProperties properties;
    private AiHealthAgentService service;

    @BeforeEach
    void setUp() {
        properties = new HealthMonitorProperties(
                true, false, "0 0 * * * *", true, 30, 20,
                new HealthMonitorProperties.Docker("tcp://docker-socket-proxy:2375"),
                new HealthMonitorProperties.History(3, 5),
                new HealthMonitorProperties.Logs(100));
        service = new AiHealthAgentService(
                chatClient, toolFactory, promptBuilder,
                reportRepository, actionRepository,
                properties, metrics, alertService);

        lenient().when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        lenient().when(promptBuilder.buildUserMessage()).thenReturn("user message");
        lenient().when(toolFactory.createTools(any(HealthToolCallTracker.class)))
                .thenReturn(new Object[0]);
        lenient().when(reportRepository.save(any(HealthReport.class))).thenAnswer(inv -> {
            HealthReport saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });
    }

    // ========== Happy path ==========

    @Test
    void should_persistReportAndOrangeProposal_when_draftContainsValidOrangeAction() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.WARNING,
                "Memoire faible detectee",
                List.of("Memoire > 80%"),
                List.of(new ProposedActionDraft(
                        ActionLevel.ORANGE,
                        ActionType.FLUSH_REDIS_CACHE,
                        Map.of("pattern", "session:*"),
                        "Liberer la memoire Redis")));
        mockDraftReturn(draft);

        // Act
        HealthReport result = service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        assertThat(result.getSeverity()).isEqualTo(ReportSeverity.WARNING);
        assertThat(result.getSummary()).isEqualTo("Memoire faible detectee");
        assertThat(result.getCycleType()).isEqualTo(HealthCycleType.SCHEDULED);
        assertThat(result.getAnomaliesJson()).contains("Memoire > 80%");

        ArgumentCaptor<HealthProposedAction> actionCaptor =
                ArgumentCaptor.forClass(HealthProposedAction.class);
        verify(actionRepository).save(actionCaptor.capture());
        HealthProposedAction saved = actionCaptor.getValue();
        assertThat(saved.getActionType()).isEqualTo(ActionType.FLUSH_REDIS_CACHE);
        assertThat(saved.getLevel()).isEqualTo(ActionLevel.ORANGE);
        assertThat(saved.getStatus()).isEqualTo(ActionStatus.PENDING);
        assertThat(saved.getJustification()).isEqualTo("Liberer la memoire Redis");
        assertThat(saved.getParamsJson()).contains("session:*");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(25));

        verify(metrics).recordCycleSuccess();
        verify(metrics).recordCycleDuration(any());
        verify(metrics).recordProposedAction(ActionLevel.ORANGE, ActionType.FLUSH_REDIS_CACHE);
        verify(alertService).maybeAlert(any(HealthReport.class));
    }

    @Test
    void should_persistRedRestartAction_when_draftProposesRestart() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.CRITICAL,
                "Backend mort",
                List.of("OOM kill detecte"),
                List.of(new ProposedActionDraft(
                        ActionLevel.RED,
                        ActionType.RESTART_SERVICE,
                        Map.of("serviceName", "spring-backend"),
                        "Redemarrage du backend necessaire")));
        mockDraftReturn(draft);

        // Act
        service.analyze(HealthCycleType.ON_DEMAND);

        // Assert
        ArgumentCaptor<HealthProposedAction> actionCaptor =
                ArgumentCaptor.forClass(HealthProposedAction.class);
        verify(actionRepository).save(actionCaptor.capture());
        HealthProposedAction saved = actionCaptor.getValue();
        assertThat(saved.getActionType()).isEqualTo(ActionType.RESTART_SERVICE);
        assertThat(saved.getLevel()).isEqualTo(ActionLevel.RED);
        assertThat(saved.getParamsJson()).contains("spring-backend");
        verify(metrics).recordProposedAction(ActionLevel.RED, ActionType.RESTART_SERVICE);
    }

    // ========== Null draft fallback ==========

    @Test
    void should_persistFallbackReport_when_draftIsNull() {
        // Arrange
        mockDraftReturn(null);

        // Act
        HealthReport result = service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        assertThat(result.getSeverity()).isEqualTo(ReportSeverity.UNKNOWN);
        assertThat(result.getSummary()).contains("draft_null");
        assertThat(result.getAnomaliesJson()).isEqualTo("[]");
        verify(reportRepository).save(any(HealthReport.class));
        verify(actionRepository, never()).save(any());
        verify(metrics, never()).recordCycleSuccess();
        verify(alertService, never()).maybeAlert(any());
    }

    // ========== Draft field defaults ==========

    @Test
    void should_useUnknownSeverity_when_draftSeverityIsNull() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                null, "resume", List.of(), List.of());
        mockDraftReturn(draft);

        // Act
        HealthReport result = service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        assertThat(result.getSeverity()).isEqualTo(ReportSeverity.UNKNOWN);
    }

    @Test
    void should_useDefaultSummary_when_draftSummaryIsNull() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.OK, null, List.of(), List.of());
        mockDraftReturn(draft);

        // Act
        HealthReport result = service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        assertThat(result.getSummary()).isEqualTo("(no summary)");
    }

    @Test
    void should_serializeEmptyAnomalies_when_draftAnomaliesIsNull() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.OK, "tout va bien", null, List.of());
        mockDraftReturn(draft);

        // Act
        HealthReport result = service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        assertThat(result.getAnomaliesJson()).isEqualTo("[]");
    }

    // ========== Proposal validation ==========

    @Test
    void should_rejectProposal_when_levelMismatchesActionType() {
        // Arrange: ORANGE type declared as RED -> mismatch
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.WARNING, "anomaly", List.of(),
                List.of(new ProposedActionDraft(
                        ActionLevel.RED,
                        ActionType.FLUSH_REDIS_CACHE,
                        Map.of(),
                        "injection attempt")));
        mockDraftReturn(draft);

        // Act
        service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        verify(actionRepository, never()).save(any());
        verify(metrics).recordProposalRejectedLevelMismatch();
    }

    @Test
    void should_skipProposal_when_typeIsNull() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.WARNING, "anomaly", List.of(),
                List.of(new ProposedActionDraft(
                        ActionLevel.ORANGE, null, Map.of(), "incomplete")));
        mockDraftReturn(draft);

        // Act
        service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        verify(actionRepository, never()).save(any());
        verify(metrics, never()).recordProposalRejectedLevelMismatch();
    }

    @Test
    void should_skipProposal_when_levelIsNull() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.WARNING, "anomaly", List.of(),
                List.of(new ProposedActionDraft(
                        null, ActionType.FLUSH_REDIS_CACHE, Map.of(), "incomplete")));
        mockDraftReturn(draft);

        // Act
        service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        verify(actionRepository, never()).save(any());
    }

    @Test
    void should_persistValidAndRejectInvalid_when_draftMixesGoodAndMismatchedProposals() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.WARNING, "anomaly", List.of(),
                List.of(
                        new ProposedActionDraft(
                                ActionLevel.ORANGE,
                                ActionType.REINDEX_ELASTICSEARCH,
                                Map.of(),
                                "reindex needed"),
                        new ProposedActionDraft(
                                ActionLevel.ORANGE,
                                ActionType.RESTART_SERVICE,
                                Map.of("serviceName", "spring-backend"),
                                "injection")));
        mockDraftReturn(draft);

        // Act
        service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        verify(actionRepository, times(1)).save(any());
        verify(metrics).recordProposalRejectedLevelMismatch();
    }

    // ========== Proposal list variants ==========

    @Test
    void should_persistReportWithoutActions_when_proposalsAreNull() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.OK, "tout va bien", List.of(), null);
        mockDraftReturn(draft);

        // Act
        service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        verify(reportRepository).save(any(HealthReport.class));
        verify(actionRepository, never()).save(any());
    }

    @Test
    void should_persistReportWithoutActions_when_proposalsAreEmpty() {
        // Arrange
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.OK, "tout va bien", List.of(), List.of());
        mockDraftReturn(draft);

        // Act
        service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        verify(actionRepository, never()).save(any());
        verify(metrics).recordCycleSuccess();
    }

    // ========== Alert service ==========

    @Test
    void should_skipAlert_when_alertServiceIsNull() {
        // Arrange
        AiHealthAgentService serviceWithoutAlerts = new AiHealthAgentService(
                chatClient, toolFactory, promptBuilder,
                reportRepository, actionRepository,
                properties, metrics, null);
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.CRITICAL, "critique", List.of(), List.of());
        mockDraftReturn(draft);

        // Act
        HealthReport result = serviceWithoutAlerts.analyze(HealthCycleType.SCHEDULED);

        // Assert
        assertThat(result.getSeverity()).isEqualTo(ReportSeverity.CRITICAL);
        verify(alertService, never()).maybeAlert(any());
        verify(metrics).recordCycleSuccess();
    }

    // ========== Tool call tracker ==========

    @Test
    void should_forwardTrackerToolsToChatClient_when_analyzeCalled() {
        // Arrange
        when(toolFactory.createTools(any(HealthToolCallTracker.class)))
                .thenReturn(new Object[]{"fake-tool"});
        HealthReportDraft draft = new HealthReportDraft(
                ReportSeverity.OK, "ok", List.of(), List.of());
        mockDraftReturn(draft);

        // Act
        service.analyze(HealthCycleType.SCHEDULED);

        // Assert
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(requestSpec).tools(captor.capture());
        assertThat(Arrays.asList(captor.getValue())).containsExactly("fake-tool");
    }

    private void mockDraftReturn(HealthReportDraft draft) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(HealthReportDraft.class)).thenReturn(draft);
    }
}
