package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
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
    private final ToolCallTracker tracker;

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
        tracker.record("searchProjects");
        log.debug("[CHAT_TOOL] searchProjects - query={}", query);
        return searchService.searchProjects(query);
    }

    /**
     * List all portfolio projects ordered by display order.
     * Use as a fallback when a keyword search yields no result or when
     * the visitor asks for a broad overview (e.g. "show me his projects",
     * "what else has he built", "voir d'autres projets").
     *
     * @return all projects with full metadata
     */
    @Tool(description = "List all portfolio projects ordered by display order. "
            + "Use this tool for broad project listing requests (e.g. 'show all projects', "
            + "'what other projects', 'voir d'autres projets') or as a fallback when "
            + "searchProjects returns no results.")
    public List<ProjectResponse> listProjects() {
        tracker.record("listProjects");
        log.debug("[CHAT_TOOL] listProjects");
        return projectService.getAllProjects();
    }

    /**
     * Retrieve the curated list of featured (highlighted) projects.
     * Featured projects are explicitly marked by Emmanuel as showcase-worthy
     * and are the canonical answer to "projets phares", "projets vedettes",
     * or "featured projects" questions.
     *
     * @return featured projects with full metadata
     */
    @Tool(description = "Get the curated list of featured (highlighted) projects. "
            + "Use this tool when the visitor asks for Emmanuel's main, flagship, "
            + "showcase or featured projects (e.g. 'projets phares', 'vitrine', "
            + "'featured projects', 'main work').")
    public List<ProjectResponse> getFeaturedProjects() {
        tracker.record("getFeaturedProjects");
        log.debug("[CHAT_TOOL] getFeaturedProjects");
        return projectService.getFeaturedProjects();
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
        tracker.record("getProjectDetails");
        log.debug("[CHAT_TOOL] getProjectDetails - projectId={}", projectId);
        return projectService.getProjectById(projectId);
    }
}
