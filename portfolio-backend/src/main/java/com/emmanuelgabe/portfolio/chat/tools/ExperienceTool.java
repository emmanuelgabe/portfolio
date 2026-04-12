package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Agent tools for querying professional experiences.
 * Supports filtering by type and retrieving recent entries.
 */
@Slf4j
@RequiredArgsConstructor
public class ExperienceTool {

    private static final int MAX_RECENT_LIMIT = 10;

    private final ExperienceService experienceService;
    private final ToolCallTracker tracker;

    /**
     * List experiences, optionally filtered by type.
     *
     * @param type experience type filter, or null for all
     * @return matching experiences ordered by display order then start date
     */
    @Tool(description = "List professional experiences, optionally filtered by type. "
            + "Types: WORK, STAGE, EDUCATION, CERTIFICATION, VOLUNTEERING.")
    public List<ExperienceResponse> listExperiences(
            @ToolParam(description = "Experience type: WORK, STAGE, EDUCATION, CERTIFICATION, "
                    + "or VOLUNTEERING. Omit to list all experiences.", required = false)
            @Nullable String type) {
        tracker.record("listExperiences");
        log.debug("[CHAT_TOOL] listExperiences - type={}", type);
        if (type != null && !type.isBlank()) {
            ExperienceType experienceType = ExperienceType.valueOf(type.toUpperCase().trim());
            return experienceService.getExperiencesByType(experienceType);
        }
        return experienceService.getAllExperiences();
    }

    /**
     * Get the most recent experiences for a quick career overview.
     *
     * @param limit maximum number of results (capped at 10)
     * @return most recent experiences
     */
    @Tool(description = "Get the most recent professional experiences. "
            + "Useful for a quick overview of Emmanuel's career.")
    public List<ExperienceResponse> getRecentExperiences(
            @ToolParam(description = "Maximum number of experiences to return (1 to 10)") int limit) {
        tracker.record("getRecentExperiences");
        int safeLimit = Math.min(Math.max(limit, 1), MAX_RECENT_LIMIT);
        log.debug("[CHAT_TOOL] getRecentExperiences - limit={}", safeLimit);
        return experienceService.getRecentExperiences(safeLimit);
    }
}
