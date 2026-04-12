package com.emmanuelgabe.portfolio.healthmonitor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the AI Health Monitor agent.
 *
 * <p>All beans in this configuration are conditional on
 * {@code ai.health-monitor.enabled=true}. When disabled, the
 * {@link HealthAgentService} bean defined in {@code HealthAgentBeanConfig}
 * falls back to {@link NoOpHealthAgentService}.</p>
 *
 * <p>The {@link DockerClient} is wired through the {@code docker-socket-proxy}
 * sidecar (see {@code docker-compose.yml}) which restricts the exposed API
 * surface to container inspect, logs, stats, and restart. The backend never
 * touches the real {@code /var/run/docker.sock}, so a backend compromise
 * cannot create privileged containers or escape to the host.</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(HealthMonitorProperties.class)
@ConditionalOnProperty(name = "ai.health-monitor.enabled", havingValue = "true")
public class HealthAgentConfig {

    @Bean
    DockerClient dockerClient(HealthMonitorProperties properties) {
        String host = properties.docker().host();
        log.info("[HEALTH_AGENT] Initializing DockerClient via proxy - host={}", host);

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .withDockerTlsVerify(false)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * RabbitAdmin is needed by the health monitor's RabbitMQ tools
     * (queue depth, dead-letter queue inspection). Spring Boot does not
     * auto-configure it, so we expose it here under the same conditional
     * as the rest of the agent so a disabled health monitor does not
     * pull RabbitAdmin into the context.
     */
    @Bean
    RabbitAdmin healthMonitorRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
