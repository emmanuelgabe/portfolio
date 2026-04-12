package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import com.emmanuelgabe.portfolio.repository.HealthReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthAgentBeanConfigTest {

    @Mock
    private Environment environment;

    @Mock
    private ObjectProvider<ChatClient> chatClientProvider;

    @Mock
    private ObjectProvider<HealthToolFactory> toolFactoryProvider;

    @Mock
    private ObjectProvider<HealthPromptBuilder> promptBuilderProvider;

    @Mock
    private ObjectProvider<HealthReportRepository> reportRepoProvider;

    @Mock
    private ObjectProvider<HealthProposedActionRepository> actionRepoProvider;

    @Mock
    private ObjectProvider<HealthMonitorProperties> propertiesProvider;

    @Mock
    private ObjectProvider<HealthMonitorMetrics> metricsProvider;

    @Mock
    private ObjectProvider<HealthAlertService> alertServiceProvider;

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

    private HealthMonitorProperties properties;
    private HealthAgentBeanConfig config;

    @BeforeEach
    void setUp() {
        config = new HealthAgentBeanConfig();
        properties = new HealthMonitorProperties(
                true, false, "0 0 * * * *", true, 30, 20,
                new HealthMonitorProperties.Docker("tcp://docker-socket-proxy:2375"),
                new HealthMonitorProperties.History(3, 5),
                new HealthMonitorProperties.Logs(100));

        lenient().when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        lenient().when(toolFactoryProvider.getIfAvailable()).thenReturn(toolFactory);
        lenient().when(promptBuilderProvider.getIfAvailable()).thenReturn(promptBuilder);
        lenient().when(reportRepoProvider.getIfAvailable()).thenReturn(reportRepository);
        lenient().when(actionRepoProvider.getIfAvailable()).thenReturn(actionRepository);
        lenient().when(propertiesProvider.getIfAvailable()).thenReturn(properties);
        lenient().when(metricsProvider.getIfAvailable()).thenReturn(metrics);
        lenient().when(alertServiceProvider.getIfAvailable()).thenReturn(alertService);
    }

    // ========== Wiring: AI enabled and all dependencies available ==========

    @Test
    void should_returnAiHealthAgentService_when_enabledAndAllDependenciesAvailable() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(AiHealthAgentService.class);
    }

    @Test
    void should_returnAiHealthAgentService_when_alertServiceUnavailableButEverythingElsePresent() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");
        when(alertServiceProvider.getIfAvailable()).thenReturn(null);

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(AiHealthAgentService.class);
    }

    // ========== Wiring: disabled ==========

    @Test
    void should_returnNoOp_when_enabledIsFalse() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("false");

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }

    @Test
    void should_returnNoOp_when_enabledPropertyIsMissing() {
        // Arrange: simulate default value from environment
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("false");

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }

    // ========== Wiring: missing required dependency ==========

    @Test
    void should_returnNoOp_when_chatClientIsUnavailable() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");
        when(chatClientProvider.getIfAvailable()).thenReturn(null);

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }

    @Test
    void should_returnNoOp_when_toolFactoryIsUnavailable() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");
        when(toolFactoryProvider.getIfAvailable()).thenReturn(null);

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }

    @Test
    void should_returnNoOp_when_promptBuilderIsUnavailable() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");
        when(promptBuilderProvider.getIfAvailable()).thenReturn(null);

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }

    @Test
    void should_returnNoOp_when_reportRepositoryIsUnavailable() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");
        when(reportRepoProvider.getIfAvailable()).thenReturn(null);

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }

    @Test
    void should_returnNoOp_when_actionRepositoryIsUnavailable() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");
        when(actionRepoProvider.getIfAvailable()).thenReturn(null);

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }

    @Test
    void should_returnNoOp_when_propertiesAreUnavailable() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");
        when(propertiesProvider.getIfAvailable()).thenReturn(null);

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }

    @Test
    void should_returnNoOp_when_metricsAreUnavailable() {
        // Arrange
        when(environment.getProperty("ai.health-monitor.enabled", "false")).thenReturn("true");
        when(metricsProvider.getIfAvailable()).thenReturn(null);

        // Act
        HealthAgentService result = config.healthAgentService(
                environment, chatClientProvider, toolFactoryProvider, promptBuilderProvider,
                reportRepoProvider, actionRepoProvider, propertiesProvider,
                metricsProvider, alertServiceProvider);

        // Assert
        assertThat(result).isInstanceOf(NoOpHealthAgentService.class);
    }
}
