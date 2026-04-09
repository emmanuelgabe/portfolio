package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.service.ProjectService;
import com.emmanuelgabe.portfolio.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * Agent tools for searching and retrieving portfolio projects.
 * Uses Elasticsearch full-text search for keyword queries and
 * the project service for detail retrieval.
 */
@Slf4j
@RequiredArgsConstructor
public class ProjectSearchTool {

    private final ProjectService projectService;
    private final SearchService searchService;

    /**
     * Search projects using Elasticsearch full-text search.
     *
     * @param query search keyword or technology name
     * @return matching projects with title, description, tech stack, and tags
     */
    @Tool(description = "Search portfolio projects by keyword or technology name. "
            + "Returns matching projects with title, description, tech stack, and tags.")
    public List<ProjectSearchResult> searchProjects(
            @ToolParam(description = "Search keyword for project title, description, or technology name")
            String query) {
        log.debug("[CHAT_TOOL] searchProjects - query={}", query);
        return searchService.searchProjects(query);
    }

    /**
     * Retrieve full details of a specific project.
     *
     * @param projectId the project ID
     * @return complete project data including URLs and images
     */
    @Tool(description = "Get full details of a specific project by its ID, "
            + "including description, tech stack, GitHub URL, and demo URL.")
    public ProjectResponse getProjectDetails(
            @ToolParam(description = "The project ID") Long projectId) {
        log.debug("[CHAT_TOOL] getProjectDetails - projectId={}", projectId);
        return projectService.getProjectById(projectId);
    }
}
