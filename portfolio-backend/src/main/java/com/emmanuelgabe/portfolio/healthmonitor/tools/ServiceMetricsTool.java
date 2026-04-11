package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Green tool: Docker live stats for a portfolio container
 * (CPU%, memory usage, network I/O).
 */
@Slf4j
public class ServiceMetricsTool {

    private static final int COLLECT_TIMEOUT_SECONDS = 5;

    private final DockerClient dockerClient;
    private final DockerServiceResolver resolver;
    private final HealthToolCallTracker tracker;

    public ServiceMetricsTool(DockerClient dockerClient, DockerServiceResolver resolver,
                              HealthToolCallTracker tracker) {
        this.dockerClient = dockerClient;
        this.resolver = resolver;
        this.tracker = tracker;
    }

    @Tool(description = "Return live Docker metrics for a portfolio service container: "
            + "CPU percent, memory usage vs limit, and cumulative network I/O. "
            + "Useful for spotting containers that are resource-starved or runaway.")
    public Map<String, Object> getServiceMetrics(
            @ToolParam(description = "The compose service name") String serviceName) {
        tracker.record("getServiceMetrics");
        log.debug("[HEALTH_TOOL] getServiceMetrics - serviceName={}", serviceName);

        if (!resolver.isAllowed(serviceName)) {
            return Map.of("error", "serviceName not allowed");
        }
        Optional<Container> container = resolver.resolve(serviceName);
        if (container.isEmpty()) {
            return Map.of("serviceName", serviceName, "state", "not_found");
        }

        AtomicReference<Statistics> statsRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        ResultCallback.Adapter<Statistics> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Statistics stats) {
                statsRef.set(stats);
                latch.countDown();
            }
        };

        try (Closeable stream = dockerClient.statsCmd(container.get().getId()).exec(callback)) {
            boolean completed = latch.await(COLLECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                return Map.of("serviceName", serviceName, "state", "timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("error", "interrupted");
        } catch (Exception e) {
            log.warn("[HEALTH_TOOL] Stats fetch failed - serviceName={}, error={}",
                    serviceName, e.getMessage());
            return Map.of("error", e.getMessage());
        }

        Statistics stats = statsRef.get();
        if (stats == null) {
            return Map.of("serviceName", serviceName, "state", "no_stats");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceName", serviceName);
        result.put("cpuPercent", computeCpuPercent(stats));
        var memoryStats = stats.getMemoryStats();
        if (memoryStats != null) {
            result.put("memoryUsageBytes", memoryStats.getUsage());
            result.put("memoryLimitBytes", memoryStats.getLimit());
        }
        var networks = stats.getNetworks();
        if (networks != null) {
            long rx = networks.values().stream()
                    .mapToLong(n -> n.getRxBytes() == null ? 0 : n.getRxBytes()).sum();
            long tx = networks.values().stream()
                    .mapToLong(n -> n.getTxBytes() == null ? 0 : n.getTxBytes()).sum();
            result.put("networkRxBytes", rx);
            result.put("networkTxBytes", tx);
        }
        return result;
    }

    private double computeCpuPercent(Statistics stats) {
        var cpuStats = stats.getCpuStats();
        var preCpuStats = stats.getPreCpuStats();
        if (cpuStats == null || preCpuStats == null) {
            return 0.0;
        }
        var cpuUsage = cpuStats.getCpuUsage();
        var preCpuUsage = preCpuStats.getCpuUsage();
        if (cpuUsage == null || preCpuUsage == null) {
            return 0.0;
        }
        Long total = cpuUsage.getTotalUsage();
        Long preTotal = preCpuUsage.getTotalUsage();
        Long sys = cpuStats.getSystemCpuUsage();
        Long preSys = preCpuStats.getSystemCpuUsage();
        if (total == null || preTotal == null || sys == null || preSys == null) {
            return 0.0;
        }
        long cpuDelta = total - preTotal;
        long sysDelta = sys - preSys;
        if (sysDelta <= 0 || cpuDelta <= 0) {
            return 0.0;
        }
        var perCpu = cpuUsage.getPercpuUsage();
        long cpuCount = perCpu != null ? perCpu.size() : 1;
        return Math.round(((double) cpuDelta / sysDelta) * cpuCount * 100.0 * 100.0) / 100.0;
    }
}
