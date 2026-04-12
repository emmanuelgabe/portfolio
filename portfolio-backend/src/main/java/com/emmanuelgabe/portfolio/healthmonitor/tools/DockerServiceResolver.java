package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves a logical compose service name (e.g. {@code spring-backend},
 * {@code redis-cache}) to the actual Docker container ID via the compose
 * label {@code com.docker.compose.service}.
 *
 * <p>Enforces a strict whitelist so the agent cannot request logs or
 * restart arbitrary containers running on the host -- only those that
 * belong to the portfolio stack.</p>
 */
@RequiredArgsConstructor
public class DockerServiceResolver {

    private static final Set<String> ALLOWED_SERVICES = Set.of(
            "spring-backend",
            "angular-frontend",
            "portfolio-db",
            "redis-cache",
            "rabbitmq",
            "kafka",
            "elasticsearch",
            "nginx-portfolio",
            "docker-socket-proxy"
    );

    private final DockerClient dockerClient;

    public boolean isAllowed(String serviceName) {
        return serviceName != null && ALLOWED_SERVICES.contains(serviceName);
    }

    public static Set<String> allowedServices() {
        return ALLOWED_SERVICES;
    }

    /**
     * Resolve a compose service name to the first matching running container.
     * Returns empty if no matching container exists or the service name is
     * not in the whitelist.
     */
    public Optional<Container> resolve(String serviceName) {
        if (!isAllowed(serviceName)) {
            return Optional.empty();
        }
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(Map.of("com.docker.compose.service", serviceName))
                .exec();
        return containers.stream().findFirst();
    }
}
