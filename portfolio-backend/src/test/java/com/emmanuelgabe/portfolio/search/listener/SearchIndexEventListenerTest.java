package com.emmanuelgabe.portfolio.search.listener;

import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.search.event.ArticleIndexEvent;
import com.emmanuelgabe.portfolio.search.event.ExperienceIndexEvent;
import com.emmanuelgabe.portfolio.search.event.ProjectIndexEvent;
import com.emmanuelgabe.portfolio.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for SearchIndexEventListener.
 */
@ExtendWith(MockitoExtension.class)
class SearchIndexEventListenerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchIndexEventListener eventListener;

    private Article testArticle;
    private Project testProject;
    private Experience testExperience;

    @BeforeEach
    void setUp() {
        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");
        testArticle.setSlug("test-article");
        testArticle.setContent("Test content");
        testArticle.setDraft(false);
        testArticle.setPublishedAt(LocalDateTime.now());

        testProject = new Project();
        testProject.setId(1L);
        testProject.setTitle("Test Project");
        testProject.setDescription("Test description");
        testProject.setTechStack("Java, Spring");
        testProject.setFeatured(true);

        testExperience = new Experience();
        testExperience.setId(1L);
        testExperience.setCompany("Tech Company");
        testExperience.setRole("Developer");
        testExperience.setType(ExperienceType.WORK);
        testExperience.setStartDate(LocalDate.of(2020, 1, 1));
    }

    // ========== Article Event Tests ==========

    @Test
    void should_indexArticle_when_articleIndexEventReceived() {
        // Arrange
        ArticleIndexEvent event = ArticleIndexEvent.forIndex(testArticle);

        // Act
        eventListener.handleArticleIndexEvent(event);

        // Assert
        verify(searchService).indexArticle(testArticle);
    }

    @Test
    void should_removeArticle_when_articleRemoveEventReceived() {
        // Arrange
        ArticleIndexEvent event = ArticleIndexEvent.forRemove(testArticle);

        // Act
        eventListener.handleArticleIndexEvent(event);

        // Assert
        verify(searchService).removeArticle(1L);
    }

    // ========== Project Event Tests ==========

    @Test
    void should_indexProject_when_projectIndexEventReceived() {
        // Arrange
        ProjectIndexEvent event = ProjectIndexEvent.forIndex(testProject);

        // Act
        eventListener.handleProjectIndexEvent(event);

        // Assert
        verify(searchService).indexProject(testProject);
    }

    @Test
    void should_removeProject_when_projectRemoveEventReceived() {
        // Arrange
        ProjectIndexEvent event = ProjectIndexEvent.forRemove(testProject);

        // Act
        eventListener.handleProjectIndexEvent(event);

        // Assert
        verify(searchService).removeProject(1L);
    }

    // ========== Experience Event Tests ==========

    @Test
    void should_indexExperience_when_experienceIndexEventReceived() {
        // Arrange
        ExperienceIndexEvent event = ExperienceIndexEvent.forIndex(testExperience);

        // Act
        eventListener.handleExperienceIndexEvent(event);

        // Assert
        verify(searchService).indexExperience(testExperience);
    }

    @Test
    void should_removeExperience_when_experienceRemoveEventReceived() {
        // Arrange
        ExperienceIndexEvent event = ExperienceIndexEvent.forRemove(testExperience);

        // Act
        eventListener.handleExperienceIndexEvent(event);

        // Assert
        verify(searchService).removeExperience(1L);
    }
}
