package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.ActiveUsersResponse;
import com.emmanuelgabe.portfolio.dto.DailyVisitorData;
import com.emmanuelgabe.portfolio.dto.VisitorStatsResponse;
import com.emmanuelgabe.portfolio.repository.DailyStatsRepository;
import com.emmanuelgabe.portfolio.service.VisitorTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Admin controller for real-time active users monitoring.
 * Provides SSE stream for live updates on the admin dashboard.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/visitors")
@RequiredArgsConstructor
public class AdminActiveUsersController {

    private final VisitorTrackingService visitorTrackingService;
    private final DailyStatsRepository dailyStatsRepository;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Stream active users count via Server-Sent Events.
     * Sends initial count immediately, then broadcasts updates every 30 seconds.
     *
     * @return SSE emitter for real-time updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamActiveUsers() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("[SSE] Client disconnected - remaining={}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("[SSE] Client timeout - remaining={}", emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("[SSE] Client error - remaining={}, error={}", emitters.size(), e.getMessage());
        });

        // Send initial count immediately
        try {
            ActiveUsersResponse response = new ActiveUsersResponse(
                    visitorTrackingService.getActiveVisitorsCount(),
                    Instant.now());
            emitter.send(SseEmitter.event()
                    .name("active-users")
                    .data(response, MediaType.APPLICATION_JSON));
            log.debug("[SSE] Initial count sent - count={}", response.count());
        } catch (IOException e) {
            emitter.completeWithError(e);
            emitters.remove(emitter);
        }

        log.info("[SSE] New client connected - total={}", emitters.size());
        return emitter;
    }

    /**
     * Broadcast active users count to all connected SSE clients.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    public void broadcastActiveUsers() {
        if (emitters.isEmpty()) {
            return;
        }

        ActiveUsersResponse response = new ActiveUsersResponse(
                visitorTrackingService.getActiveVisitorsCount(),
                Instant.now());

        log.debug("[SSE] Broadcasting to {} clients - count={}", emitters.size(), response.count());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("active-users")
                        .data(response, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitters.remove(emitter);
                log.debug("[SSE] Failed to send, removing client - error={}", e.getMessage());
            }
        }
    }

    /**
     * Get current active users count (non-streaming).
     *
     * @return active users count response
     */
    @GetMapping("/count")
    public ResponseEntity<ActiveUsersResponse> getActiveUsersCount() {
        int count = visitorTrackingService.getActiveVisitorsCount();
        log.debug("[GET_ACTIVE_USERS] Count retrieved - count={}", count);
        return ResponseEntity.ok(new ActiveUsersResponse(count, Instant.now()));
    }

    /**
     * Get visitor statistics including current active count and last month's total.
     *
     * @return visitor stats response with active count and last month total
     */
    @GetMapping("/stats")
    public ResponseEntity<VisitorStatsResponse> getVisitorStats() {
        int activeCount = visitorTrackingService.getActiveVisitorsCount();

        // Calculate last month's date range
        LocalDate today = LocalDate.now();
        LocalDate firstDayLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayLastMonth = today.withDayOfMonth(1).minusDays(1);

        long lastMonthCount = dailyStatsRepository.sumUniqueVisitorsBetween(
                firstDayLastMonth, lastDayLastMonth);

        log.debug("[GET_VISITOR_STATS] Stats retrieved - activeCount={}, lastMonthCount={}",
                activeCount, lastMonthCount);

        return ResponseEntity.ok(new VisitorStatsResponse(
                activeCount,
                lastMonthCount,
                Instant.now()));
    }

    /**
     * Get daily visitor data for the last 7 days.
     * Returns unique visitors count from DailyStats for each day.
     *
     * @return list of daily visitor counts
     */
    @GetMapping("/daily")
    public ResponseEntity<List<DailyVisitorData>> getDailyVisitorData() {
        log.debug("[GET_DAILY_VISITORS] Fetching daily visitor data for last 7 days");

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        // Get stats from database
        var statsMap = dailyStatsRepository.findByStatsDateBetweenOrderByStatsDateDesc(weekAgo, today)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        stats -> stats.getStatsDate(),
                        stats -> stats.getUniqueVisitors()
                ));

        // Generate 7 days of data (oldest to newest)
        List<DailyVisitorData> dailyData = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = statsMap.getOrDefault(date, 0L);
            dailyData.add(new DailyVisitorData(date, count));
        }

        log.debug("[GET_DAILY_VISITORS] Data retrieved - dataPoints={}", dailyData.size());
        return ResponseEntity.ok(dailyData);
    }
}
