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
     * Create all chat agent tools. The vector store is passed as a parameter
     * because it is a conditional bean (only available when AI is enabled).
     *
     * @param vectorStore the Elasticsearch vector store for semantic search
     * @return array of tool instances ready for Spring AI function calling
     */
    public Object[] createTools(VectorStore vectorStore) {
        return new Object[]{
            new ProjectSearchTool(projectService, searchService),
            new ArticleSearchTool(articleService),
            new ExperienceTool(experienceService),
            new SkillTool(skillService),
            new SiteInfoTool(siteConfigService, cvService),
            new ContactPrepareTool(),
            new PortfolioSearchTool(vectorStore)
        };
    }
}
