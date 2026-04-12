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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChatToolFactoryTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private ArticleService articleService;

    @Mock
    private ExperienceService experienceService;

    @Mock
    private SkillService skillService;

    @Mock
    private SiteConfigurationService siteConfigService;

    @Mock
    private CvService cvService;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private ChatToolFactory chatToolFactory;

    // ========== createTools ==========

    @Test
    void should_createSevenTools_when_createToolsCalled() {
        // Arrange
        VectorStore vectorStore = mock(VectorStore.class);

        // Act
        Object[] tools = chatToolFactory.createTools(vectorStore);

        // Assert
        assertThat(tools).hasSize(7);
    }

    @Test
    void should_createCorrectToolTypes_when_createToolsCalled() {
        // Arrange
        VectorStore vectorStore = mock(VectorStore.class);

        // Act
        Object[] tools = chatToolFactory.createTools(vectorStore);

        // Assert
        assertThat(Arrays.stream(tools).map(Object::getClass).toList())
            .containsExactly(
                ProjectSearchTool.class,
                ArticleSearchTool.class,
                ExperienceTool.class,
                SkillTool.class,
                SiteInfoTool.class,
                ContactPrepareTool.class,
                PortfolioSearchTool.class
            );
    }
}
