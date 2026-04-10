package com.emmanuelgabe.portfolio.chat;

import com.emmanuelgabe.portfolio.chat.tools.ArticleSearchTool;
import com.emmanuelgabe.portfolio.chat.tools.ContactPrepareTool;
import com.emmanuelgabe.portfolio.chat.tools.ExperienceTool;
import com.emmanuelgabe.portfolio.chat.tools.PortfolioSearchTool;
import com.emmanuelgabe.portfolio.chat.tools.ProjectSearchTool;
import com.emmanuelgabe.portfolio.chat.tools.SiteInfoTool;
import com.emmanuelgabe.portfolio.chat.tools.SkillTool;
import com.emmanuelgabe.portfolio.service.ArticleService;
import com.emmanuelgabe.portfolio.service.CvService;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import com.emmanuelgabe.portfolio.service.ProjectService;
import com.emmanuelgabe.portfolio.service.SearchService;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import com.emmanuelgabe.portfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * Factory that creates chat agent tool instances.
 * Tool objects are plain POJOs with {@code @Tool}-annotated methods -- they are
 * not Spring beans themselves but hold references to Spring services for data access.
 *
 * <p>Separated from {@link ChatConfig} to keep the bean factory method parameter
 * count within checkstyle limits and improve testability.</p>
 */
@Component
@RequiredArgsConstructor
public class ChatToolFactory {

    private final ProjectService projectService;
    private final ArticleService articleService;
    private final ExperienceService experienceService;
    private final SkillService skillService;
    private final SiteConfigurationService siteConfigService;
    private final CvService cvService;
    private final SearchService searchService;

    /**
     * Create all chat agent tools without call tracking.
     * Used by the synchronous endpoint and in tests.
     *
     * @param vectorStore the Elasticsearch vector store for semantic search
     * @return array of tool instances ready for Spring AI function calling
     */
    public Object[] createTools(VectorStore vectorStore) {
        return createTools(vectorStore, ToolCallTracker.noop());
    }

    /**
     * Create all chat agent tools with call tracking for the streaming endpoint.
     * The tracker records which tools were invoked so the SSE stream can include
     * tool call metadata for the frontend.
     *
     * @param vectorStore the Elasticsearch vector store for semantic search
     * @param tracker     per-request tracker for tool call metadata
     * @return array of tool instances ready for Spring AI function calling
     */
    public Object[] createTools(VectorStore vectorStore, ToolCallTracker tracker) {
        return new Object[]{
            new ProjectSearchTool(projectService, searchService, tracker),
            new ArticleSearchTool(articleService, tracker),
            new ExperienceTool(experienceService, tracker),
            new SkillTool(skillService, tracker),
            new SiteInfoTool(siteConfigService, cvService, tracker),
            new ContactPrepareTool(tracker),
            new PortfolioSearchTool(vectorStore, tracker)
        };
    }
}
