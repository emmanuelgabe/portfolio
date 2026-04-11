package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background sweeper that transitions stale {@code PENDING} actions to
 * {@code EXPIRED} once their validity window elapses. Prevents the admin
 * from approving proposals whose context (logs, metrics snapshot) has
 * drifted far enough from the current system state to be unsafe.
 *
 * <p>The expiration window itself is set on each proposal when it is
 * persisted by {@link AiHealthAgentService}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.health-monitor.enabled", havingValue = "true")
public class ActionExpirationScheduler {

    private final HealthProposedActionRepository actionRepository;
    private final HealthMonitorMetrics metrics;

    @Scheduled(fixedRate = 5 * 60 * 1000L)
    @Transactional
    public void expireStaleActions() {
        LocalDateTime now = LocalDateTime.now();
        List<HealthProposedAction> stale = actionRepository
                .findByStatusAndExpiresAtBefore(ActionStatus.PENDING, now);
        if (stale.isEmpty()) {
            return;
        }
        int expired = actionRepository.expireStaleActions(now);
        stale.forEach(a -> metrics.recordActionExpired(a.getActionType()));
        log.info("[HEALTH_MONITOR] Expired stale pending actions - count={}", expired);
    }
}
