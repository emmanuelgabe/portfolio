package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Green tool: reports the Docker health and running state of a portfolio
 * service container. Read-only, no confirmation required.
 */
@Slf4j
public class ServiceHealthTool {

    private final DockerClient dockerClient;
    private final DockerServiceResolver resolver;
    private final HealthToolCallTracker tracker;

    public ServiceHealthTool(DockerClient dockerClient, DockerServiceResolver resolver,
                             HealthToolCallTracker tracker) {
        this.dockerClient = dockerClient;
        this.resolver = resolver;
        this.tracker = tracker;
    }

    @Tool(description = "Check the Docker health and running state of a portfolio service container. "
            + "Allowed serviceName values: spring-backend, angular-frontend, portfolio-db, "
            + "redis-cache, rabbitmq, kafka, elasticsearch, nginx-portfolio, docker-socket-proxy. "
            + "Returns the container state (running, exited, restarting), health check status, "
            + "restart count, and started-at timestamp.")
    public Map<String, Object> checkServiceHealth(
            @ToolParam(description = "The compose service name to inspect") String serviceName) {
        tracker.record("checkServiceHealth");
        log.debug("[HEALTH_TOOL] checkServiceHealth - serviceName={}", serviceName);

        if (!resolver.isAllowed(serviceName)) {
            return Map.of("error", "serviceName not allowed",
                    "allowed", DockerServiceResolver.allowedServices());
        }

        Optional<Container> container = resolver.resolve(serviceName);
        if (container.isEmpty()) {
            return Map.of("serviceName", serviceName, "state", "not_found");
        }

        InspectContainerResponse inspect = dockerClient
                .inspectContainerCmd(container.get().getId())
                .exec();
        InspectContainerResponse.ContainerState state = inspect.getState();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceName", serviceName);
        result.put("containerId", inspect.getId().substring(0, 12));
        result.put("state", state.getStatus());
        result.put("running", Boolean.TRUE.equals(state.getRunning()));
        result.put("restartCount", inspect.getRestartCount());
        result.put("startedAt", state.getStartedAt());
        result.put("exitCode", state.getExitCodeLong());
        if (state.getHealth() != null) {
            result.put("healthStatus", state.getHealth().getStatus());
            result.put("healthFailingStreak", state.getHealth().getFailingStreak());
        }
        return result;
    }
}
