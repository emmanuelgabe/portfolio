package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceHealthToolTest {

    @Mock
    private DockerClient dockerClient;

    @Mock
    private DockerServiceResolver resolver;

    private HealthToolCallTracker tracker;
    private ServiceHealthTool serviceHealthTool;

    @BeforeEach
    void setUp() {
        tracker = new HealthToolCallTracker();
        serviceHealthTool = new ServiceHealthTool(dockerClient, resolver, tracker);
    }

    // ========== Service name validation ==========

    @Test
    void should_returnError_when_checkServiceHealthCalledWithDisallowedName() {
        // Arrange
        when(resolver.isAllowed("evil-service")).thenReturn(false);

        // Act
        Map<String, Object> result = serviceHealthTool.checkServiceHealth("evil-service");

        // Assert
        assertThat(result).containsEntry("error", "serviceName not allowed");
        assertThat(result).containsKey("allowed");
        assertThat(tracker.getCalls()).containsExactly("checkServiceHealth");
    }

    @Test
    void should_returnNotFound_when_checkServiceHealthCalledAndContainerMissing() {
        // Arrange
        when(resolver.isAllowed("redis-cache")).thenReturn(true);
        when(resolver.resolve("redis-cache")).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = serviceHealthTool.checkServiceHealth("redis-cache");

        // Assert
        assertThat(result).containsEntry("serviceName", "redis-cache");
        assertThat(result).containsEntry("state", "not_found");
    }

    // ========== Happy path ==========

    @Test
    void should_returnContainerState_when_checkServiceHealthFindsRunningContainer() {
        // Arrange
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("abcd1234567890");

        InspectContainerResponse inspect = mock(InspectContainerResponse.class);
        InspectContainerResponse.ContainerState state = mock(InspectContainerResponse.ContainerState.class);
        lenient().when(inspect.getId()).thenReturn("abcd1234567890abcdef");
        lenient().when(inspect.getState()).thenReturn(state);
        lenient().when(inspect.getRestartCount()).thenReturn(0);
        lenient().when(state.getStatus()).thenReturn("running");
        lenient().when(state.getRunning()).thenReturn(Boolean.TRUE);
        lenient().when(state.getStartedAt()).thenReturn("2026-04-11T10:00:00Z");
        lenient().when(state.getExitCodeLong()).thenReturn(0L);
        lenient().when(state.getHealth()).thenReturn(null);

        InspectContainerCmd cmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("abcd1234567890")).thenReturn(cmd);
        when(cmd.exec()).thenReturn(inspect);

        when(resolver.isAllowed("spring-backend")).thenReturn(true);
        when(resolver.resolve("spring-backend")).thenReturn(Optional.of(container));

        // Act
        Map<String, Object> result = serviceHealthTool.checkServiceHealth("spring-backend");

        // Assert
        assertThat(result).containsEntry("serviceName", "spring-backend");
        assertThat(result).containsEntry("state", "running");
        assertThat(result).containsEntry("running", Boolean.TRUE);
        assertThat(result).containsEntry("restartCount", 0);
        assertThat(result).containsEntry("exitCode", 0L);
        assertThat(result.get("containerId")).isEqualTo("abcd12345678");
        assertThat(result).doesNotContainKey("healthStatus");
    }

    @Test
    void should_includeHealthFields_when_containerExposesHealthCheck() {
        // Arrange
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("cafebabefeed");

        InspectContainerResponse inspect = mock(InspectContainerResponse.class);
        InspectContainerResponse.ContainerState state = mock(InspectContainerResponse.ContainerState.class);
        HealthState health = mock(HealthState.class);
        lenient().when(inspect.getId()).thenReturn("cafebabefeed1234567890");
        lenient().when(inspect.getState()).thenReturn(state);
        lenient().when(inspect.getRestartCount()).thenReturn(2);
        lenient().when(state.getStatus()).thenReturn("running");
        lenient().when(state.getRunning()).thenReturn(Boolean.TRUE);
        lenient().when(state.getStartedAt()).thenReturn("2026-04-11T09:00:00Z");
        lenient().when(state.getExitCodeLong()).thenReturn(0L);
        lenient().when(state.getHealth()).thenReturn(health);
        lenient().when(health.getStatus()).thenReturn("healthy");
        lenient().when(health.getFailingStreak()).thenReturn(0);

        InspectContainerCmd cmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("cafebabefeed")).thenReturn(cmd);
        when(cmd.exec()).thenReturn(inspect);

        when(resolver.isAllowed("portfolio-db")).thenReturn(true);
        when(resolver.resolve("portfolio-db")).thenReturn(Optional.of(container));

        // Act
        Map<String, Object> result = serviceHealthTool.checkServiceHealth("portfolio-db");

        // Assert
        assertThat(result).containsEntry("healthStatus", "healthy");
        assertThat(result).containsEntry("healthFailingStreak", 0);
        assertThat(result).containsEntry("restartCount", 2);
    }
}
