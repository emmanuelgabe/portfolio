package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Agent tool for querying technical skills by category.
 */
@Slf4j
@RequiredArgsConstructor
public class SkillTool {

    private final SkillService skillService;
    private final ToolCallTracker tracker;

    /**
     * List skills, optionally filtered by category.
     *
     * @param category skill category filter, or null for all
     * @return matching skills ordered by display order
     */
    @Tool(description = "List technical skills, optionally filtered by category. "
            + "Categories: FRONTEND, BACKEND, DATABASE, DEVOPS, TOOLS.")
    public List<SkillResponse> listSkills(
            @ToolParam(description = "Skill category: FRONTEND, BACKEND, DATABASE, DEVOPS, "
                    + "or TOOLS. Omit to list all skills.", required = false)
            @Nullable String category) {
        tracker.record("listSkills");
        log.debug("[CHAT_TOOL] listSkills - category={}", category);
        if (category != null && !category.isBlank()) {
            SkillCategory skillCategory = SkillCategory.valueOf(category.toUpperCase().trim());
            return skillService.getSkillsByCategory(skillCategory);
        }
        return skillService.getAllSkills();
    }
}
