package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import com.emmanuelgabe.portfolio.repository.HealthReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Always-loaded bean selector for the health agent service.
 *
 * <p>When {@code ai.health-monitor.enabled=true} and all Spring AI dependencies
 * are available, creates the real {@link AiHealthAgentService}. Otherwise
 * returns a {@link NoOpHealthAgentService} so downstream wiring (controller,
 * scheduler) always has a bean to inject and can be disabled at the property
 * level without broken autowiring.</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(HealthMonitorProperties.class)
public class HealthAgentBeanConfig {

    @Bean
    HealthAgentService healthAgentService(
            Environment environment,
            ObjectProvider<ChatClient> chatClientProvider,
            ObjectProvider<HealthToolFactory> toolFactoryProvider,
            ObjectProvider<HealthPromptBuilder> promptBuilderProvider,
            ObjectProvider<HealthReportRepository> reportRepoProvider,
            ObjectProvider<HealthProposedActionRepository> actionRepoProvider,
            ObjectProvider<HealthMonitorProperties> propertiesProvider,
            ObjectProvider<HealthMonitorMetrics> metricsProvider,
            ObjectProvider<HealthAlertService> alertServiceProvider) {

        boolean enabled = Boolean.parseBoolean(
                environment.getProperty("ai.health-monitor.enabled", "false"));
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        HealthToolFactory toolFactory = toolFactoryProvider.getIfAvailable();
        HealthPromptBuilder promptBuilder = promptBuilderProvider.getIfAvailable();
        HealthReportRepository reportRepo = reportRepoProvider.getIfAvailable();
        HealthProposedActionRepository actionRepo = actionRepoProvider.getIfAvailable();
        HealthMonitorProperties properties = propertiesProvider.getIfAvailable();
        HealthMonitorMetrics metrics = metricsProvider.getIfAvailable();
        HealthAlertService alertService = alertServiceProvider.getIfAvailable();

        if (enabled && chatClient != null && toolFactory != null && promptBuilder != null
                && reportRepo != null && actionRepo != null && properties != null
                && metrics != null) {
            log.info("[HEALTH_AGENT] Wiring AiHealthAgentService - alertServiceAvailable={}",
                    alertService != null);
            return new AiHealthAgentService(chatClient, toolFactory, promptBuilder,
                    reportRepo, actionRepo, properties, metrics, alertService);
        }
        log.info("[HEALTH_AGENT] Wiring NoOpHealthAgentService (enabled={})", enabled);
        return new NoOpHealthAgentService();
    }
}
