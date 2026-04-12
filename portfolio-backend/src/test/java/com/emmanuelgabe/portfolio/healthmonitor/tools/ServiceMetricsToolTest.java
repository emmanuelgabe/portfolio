package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.StatisticNetworksConfig;
import com.github.dockerjava.api.model.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceMetricsToolTest {

    @Mock
    private DockerClient dockerClient;

    @Mock
    private DockerServiceResolver resolver;

    private HealthToolCallTracker tracker;
    private ServiceMetricsTool tool;

    @BeforeEach
    void setUp() {
        tracker = new HealthToolCallTracker();
        tool = new ServiceMetricsTool(dockerClient, resolver, tracker);
    }

    // ========== Service name validation ==========

    @Test
    void should_returnError_when_getServiceMetricsCalledWithDisallowedName() {
        // Arrange
        when(resolver.isAllowed("evil-service")).thenReturn(false);

        // Act
        Map<String, Object> result = tool.getServiceMetrics("evil-service");

        // Assert
        assertThat(result).containsEntry("error", "serviceName not allowed");
        assertThat(tracker.getCalls()).containsExactly("getServiceMetrics");
    }

    @Test
    void should_returnNotFound_when_getServiceMetricsCalledAndContainerMissing() {
        // Arrange
        when(resolver.isAllowed("redis-cache")).thenReturn(true);
        when(resolver.resolve("redis-cache")).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = tool.getServiceMetrics("redis-cache");

        // Assert
        assertThat(result).containsEntry("serviceName", "redis-cache");
        assertThat(result).containsEntry("state", "not_found");
    }

    // ========== Happy path ==========

    @Test
    void should_returnMemoryAndNetwork_when_getServiceMetricsFindsRunningContainer() {
        // Arrange
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("abc123");
        when(resolver.isAllowed("spring-backend")).thenReturn(true);
        when(resolver.resolve("spring-backend")).thenReturn(Optional.of(container));

        MemoryStatsConfig memoryStats = mock(MemoryStatsConfig.class);
        lenient().when(memoryStats.getUsage()).thenReturn(128L * 1024 * 1024);
        lenient().when(memoryStats.getLimit()).thenReturn(512L * 1024 * 1024);

        StatisticNetworksConfig networkConfig = mock(StatisticNetworksConfig.class);
        lenient().when(networkConfig.getRxBytes()).thenReturn(1000L);
        lenient().when(networkConfig.getTxBytes()).thenReturn(2000L);

        Statistics stats = mock(Statistics.class);
        lenient().when(stats.getMemoryStats()).thenReturn(memoryStats);
        lenient().when(stats.getNetworks()).thenReturn(Map.of("eth0", networkConfig));
        lenient().when(stats.getCpuStats()).thenReturn(null);
        lenient().when(stats.getPreCpuStats()).thenReturn(null);

        StatsCmd cmd = mock(StatsCmd.class);
        when(dockerClient.statsCmd("abc123")).thenReturn(cmd);
        when(cmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback.Adapter<Statistics> callback = invocation.getArgument(0);
            callback.onNext(stats);
            return callback;
        });

        // Act
        Map<String, Object> result = tool.getServiceMetrics("spring-backend");

        // Assert
        assertThat(result).containsEntry("serviceName", "spring-backend");
        assertThat(result).containsEntry("cpuPercent", 0.0);
        assertThat(result).containsEntry("memoryUsageBytes", 128L * 1024 * 1024);
        assertThat(result).containsEntry("memoryLimitBytes", 512L * 1024 * 1024);
        assertThat(result).containsEntry("networkRxBytes", 1000L);
        assertThat(result).containsEntry("networkTxBytes", 2000L);
    }

    @Test
    void should_returnNoStats_when_statsCmdCallbackReturnsNullStats() {
        // Arrange
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("abc123");
        when(resolver.isAllowed("spring-backend")).thenReturn(true);
        when(resolver.resolve("spring-backend")).thenReturn(Optional.of(container));

        StatsCmd cmd = mock(StatsCmd.class);
        when(dockerClient.statsCmd("abc123")).thenReturn(cmd);
        when(cmd.exec(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultCallback.Adapter<Statistics> callback = invocation.getArgument(0);
            callback.onNext(null);
            return callback;
        });

        // Act
        Map<String, Object> result = tool.getServiceMetrics("spring-backend");

        // Assert
        assertThat(result).containsEntry("state", "no_stats");
    }

    @Test
    void should_returnError_when_statsCmdThrows() {
        // Arrange
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("abc123");
        when(resolver.isAllowed("spring-backend")).thenReturn(true);
        when(resolver.resolve("spring-backend")).thenReturn(Optional.of(container));

        StatsCmd cmd = mock(StatsCmd.class);
        when(dockerClient.statsCmd("abc123")).thenReturn(cmd);
        when(cmd.exec(any())).thenThrow(new RuntimeException("docker unreachable"));

        // Act
        Map<String, Object> result = tool.getServiceMetrics("spring-backend");

        // Assert
        assertThat(result).containsEntry("error", "docker unreachable");
    }
}
