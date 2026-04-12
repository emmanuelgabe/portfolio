package com.emmanuelgabe.portfolio.dto.health;

import com.emmanuelgabe.portfolio.healthmonitor.action.ActionLevel;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionType;

import java.time.LocalDateTime;
import java.util.Map;

public record ProposedActionResponse(
        Long id,
        Long reportId,
        ActionLevel level,
        ActionType actionType,
        Map<String, Object> params,
        String justification,
        ActionStatus status,
        String approvedBy,
        LocalDateTime approvedAt,
        LocalDateTime executedAt,
        String rejectionReason,
        String resultMessage,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) { }
