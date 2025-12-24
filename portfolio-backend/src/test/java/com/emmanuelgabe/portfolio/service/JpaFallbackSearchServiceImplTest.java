package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.search.ArticleSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ExperienceSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.search.mapper.SearchDocumentMapper;
import com.emmanuelgabe.portfolio.service.impl.JpaFallbackSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JpaFallbackSearchServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class JpaFallbackSearchServiceImplTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private SearchDocumentMapper searchDocumentMapper;

    @InjectMocks
    private JpaFallbackSearchServiceImpl searchService;

    private Article testArticle;
    private Project testProject;
    private Experience testExperience;
    private ArticleSearchResult testArticleResult;
    private ProjectSearchResult testProjectResult;
    private ExperienceSearchResult testExperienceResult;

    @BeforeEach
    void setUp() {
        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");
        testArticle.setSlug("test-article");
        testArticle.setContent("Test content about Java programming");
        testArticle.setExcerpt("Test excerpt");
        testArticle.setDraft(false);
        testArticle.setPublishedAt(LocalDateTime.now());

        testProject = new Project();
        testProject.setId(1L);
        testProject.setTitle("Test Project");
        testProject.setDescription("A test project description");
        testProject.setTechStack("Java, Spring Boot, PostgreSQL");
        testProject.setFeatured(true);

        testExperience = new Experience();
        testExperience.setId(1L);
        testExperience.setCompany("Tech Company");
        testExperience.setRole("Senior Developer");
        testExperience.setType(ExperienceType.WORK);
        testExperience.setStartDate(LocalDate.of(2020, 1, 1));
        testExperience.setEndDate(LocalDate.of(2023, 12, 31));

        // Create search result DTOs
        testArticleResult = new ArticleSearchResult();
        testArticleResult.setId(1L);
        testArticleResult.setTitle("Test Article");
        testArticleResult.setSlug("test-article");
        testArticleResult.setExcerpt("Test excerpt");

        testProjectResult = new ProjectSearchResult();
        testProjectResult.setId(1L);
        testProjectResult.setTitle("Test Project");
        testProjectResult.setTechStack("Java, Spring Boot, PostgreSQL");

        testExperienceResult = new ExperienceSearchResult();
        testExperienceResult.setId(1L);
        testExperienceResult.setCompany("Tech Company");
        testExperienceResult.setRole("Senior Developer");
    }

    // ========== Search Articles Tests ==========

    @Test
    void should_returnArticleResults_when_searchArticlesCalledWithMatchingQuery() {
        // Arrange
        String query = "java";
        when(articleRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(query, query))
                .thenReturn(List.of(testArticle));
        when(searchDocumentMapper.toSearchResult(testArticle)).thenReturn(testArticleResult);

        // Act
        List<ArticleSearchResult> results = searchService.searchArticles(query);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(0).getTitle()).isEqualTo("Test Article");
        assertThat(results.get(0).getSlug()).isEqualTo("test-article");

        verify(articleRepository).findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(query, query);
        verify(searchDocumentMapper).toSearchResult(testArticle);
    }

    @Test
    void should_returnEmptyList_when_searchArticlesCalledWithNoMatches() {
        // Arrange
        String query = "nonexistent";
        when(articleRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(query, query))
                .thenReturn(List.of());

        // Act
        List<ArticleSearchResult> results = searchService.searchArticles(query);

        // Assert
        assertThat(results).isEmpty();

        verify(articleRepository).findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(query, query);
    }

    // ========== Search Projects Tests ==========

    @Test
    void should_returnProjectResults_when_searchProjectsCalledWithMatchingQuery() {
        // Arrange
        String query = "spring";
        when(projectRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query))
                .thenReturn(List.of(testProject));
        when(searchDocumentMapper.toSearchResult(testProject)).thenReturn(testProjectResult);

        // Act
        List<ProjectSearchResult> results = searchService.searchProjects(query);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(0).getTitle()).isEqualTo("Test Project");
        assertThat(results.get(0).getTechStack()).isEqualTo("Java, Spring Boot, PostgreSQL");

        verify(projectRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
        verify(searchDocumentMapper).toSearchResult(testProject);
    }

    @Test
    void should_returnEmptyList_when_searchProjectsCalledWithNoMatches() {
        // Arrange
        String query = "nonexistent";
        when(projectRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query))
                .thenReturn(List.of());

        // Act
        List<ProjectSearchResult> results = searchService.searchProjects(query);

        // Assert
        assertThat(results).isEmpty();

        verify(projectRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
    }

    // ========== Search Experiences Tests ==========

    @Test
    void should_returnExperienceResults_when_searchExperiencesCalledWithMatchingQuery() {
        // Arrange
        String query = "developer";
        when(experienceRepository.findByCompanyContainingIgnoreCaseOrRoleContainingIgnoreCase(query, query))
                .thenReturn(List.of(testExperience));
        when(searchDocumentMapper.toSearchResult(testExperience)).thenReturn(testExperienceResult);

        // Act
        List<ExperienceSearchResult> results = searchService.searchExperiences(query);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(0).getCompany()).isEqualTo("Tech Company");
        assertThat(results.get(0).getRole()).isEqualTo("Senior Developer");

        verify(experienceRepository).findByCompanyContainingIgnoreCaseOrRoleContainingIgnoreCase(query, query);
        verify(searchDocumentMapper).toSearchResult(testExperience);
    }

    @Test
    void should_returnEmptyList_when_searchExperiencesCalledWithNoMatches() {
        // Arrange
        String query = "nonexistent";
        when(experienceRepository.findByCompanyContainingIgnoreCaseOrRoleContainingIgnoreCase(query, query))
                .thenReturn(List.of());

        // Act
        List<ExperienceSearchResult> results = searchService.searchExperiences(query);

        // Assert
        assertThat(results).isEmpty();

        verify(experienceRepository).findByCompanyContainingIgnoreCaseOrRoleContainingIgnoreCase(query, query);
    }

    // ========== Reindex Tests ==========

    @Test
    void should_returnZero_when_reindexAllCalledInFallbackMode() {
        // Act
        int result = searchService.reindexAll();

        // Assert
        assertThat(result).isZero();
    }

    // ========== Index/Remove Tests ==========

    @Test
    void should_doNothing_when_indexArticleCalledInFallbackMode() {
        // Act - should not throw
        searchService.indexArticle(testArticle);

        // Assert - no exception thrown, method is no-op
    }

    @Test
    void should_doNothing_when_removeArticleCalledInFallbackMode() {
        // Act - should not throw
        searchService.removeArticle(1L);

        // Assert - no exception thrown, method is no-op
    }

    @Test
    void should_doNothing_when_indexProjectCalledInFallbackMode() {
        // Act - should not throw
        searchService.indexProject(testProject);

        // Assert - no exception thrown, method is no-op
    }

    @Test
    void should_doNothing_when_removeProjectCalledInFallbackMode() {
        // Act - should not throw
        searchService.removeProject(1L);

        // Assert - no exception thrown, method is no-op
    }

    @Test
    void should_doNothing_when_indexExperienceCalledInFallbackMode() {
        // Act - should not throw
        searchService.indexExperience(testExperience);

        // Assert - no exception thrown, method is no-op
    }

    @Test
    void should_doNothing_when_removeExperienceCalledInFallbackMode() {
        // Act - should not throw
        searchService.removeExperience(1L);

        // Assert - no exception thrown, method is no-op
    }
}
