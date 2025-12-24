package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.service.VisitorTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Redis-based implementation of VisitorTrackingService.
 * Stores visitor sessions as individual keys with TTL for automatic cleanup.
 * Also tracks daily unique visitors using Redis Sets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorTrackingServiceImpl implements VisitorTrackingService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "visitor:";
    private static final String DAILY_SET_PREFIX = "visitors:daily:";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final Duration DAILY_SET_TTL = Duration.ofDays(7);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public void registerHeartbeat(String sessionId) {
        // Update active visitor TTL
        String key = KEY_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), TTL);

        // Track daily unique visitor
        String dailyKey = DAILY_SET_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        redisTemplate.opsForSet().add(dailyKey, sessionId);
        redisTemplate.expire(dailyKey, DAILY_SET_TTL);

        log.debug("[VISITOR] Heartbeat registered - sessionId={}", sessionId);
    }

    @Override
    public int getActiveVisitorsCount() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        int count = keys != null ? keys.size() : 0;
        log.debug("[VISITOR] Active visitors count - count={}", count);
        return count;
    }

    @Override
    public long getDailyUniqueVisitorsCount(LocalDate date) {
        String dailyKey = DAILY_SET_PREFIX + date.format(DATE_FORMATTER);
        Long count = redisTemplate.opsForSet().size(dailyKey);
        long result = count != null ? count : 0L;
        log.debug("[VISITOR] Daily unique visitors - date={}, count={}", date, result);
        return result;
    }

    @Override
    public void clearDailyUniqueVisitors(LocalDate date) {
        String dailyKey = DAILY_SET_PREFIX + date.format(DATE_FORMATTER);
        Boolean deleted = redisTemplate.delete(dailyKey);
        log.info("[VISITOR] Cleared daily unique visitors - date={}, deleted={}", date, deleted);
    }
}
