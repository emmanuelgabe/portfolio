package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.service.impl.DocumentIngestionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private MarkdownService markdownService;

    @Captor
    private ArgumentCaptor<List<Document>> documentsCaptor;

    @InjectMocks
    private DocumentIngestionServiceImpl documentIngestionService;

    // ========== Ingest Article Tests ==========

    @Test
    void should_addDocumentToVectorStore_when_ingestArticleCalledWithPublishedArticle() {
        // Arrange
        Article article = createPublishedArticle(1L, "Spring Boot Guide", "spring-boot-guide",
                "Learn Spring Boot", "A comprehensive guide", Set.of(createTag("Java"), createTag("Spring")));
        when(articleRepository.findByIdWithTags(1L)).thenReturn(Optional.of(article));
        when(markdownService.renderToHtml("Learn Spring Boot")).thenReturn("<p>Learn Spring Boot</p>");

        // Act
        documentIngestionService.ingestArticle(article);

        // Assert
        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> docs = documentsCaptor.getValue();
        assertThat(docs).hasSize(1);

        Document doc = docs.get(0);
        assertThat(doc.getId()).isEqualTo("article-1");
        assertThat(doc.getMetadata().get("type")).isEqualTo("article");
        assertThat(doc.getMetadata().get("title")).isEqualTo("Spring Boot Guide");
        assertThat(doc.getMetadata().get("slug")).isEqualTo("spring-boot-guide");
        assertThat(doc.getText()).contains("Spring Boot Guide");
    }

    @Test
    void should_includeTagsInDocument_when_ingestArticleCalledWithTaggedArticle() {
        // Arrange
        Article article = createPublishedArticle(1L, "Test", "test", "Content", null,
                Set.of(createTag("Java"), createTag("Spring")));
        when(articleRepository.findByIdWithTags(1L)).thenReturn(Optional.of(article));
        when(markdownService.renderToHtml("Content")).thenReturn("<p>Content</p>");

        // Act
        documentIngestionService.ingestArticle(article);

        // Assert
        verify(vectorStore).add(documentsCaptor.capture());
        String text = documentsCaptor.getValue().get(0).getText();
        assertThat(text).contains("Tags:");
        assertThat(text).contains("Java");
        assertThat(text).contains("Spring");
    }

    @Test
    void should_removeEmbedding_when_ingestArticleCalledWithUnpublishedArticle() {
        // Arrange
        Article article = createDraftArticle(2L);
        when(articleRepository.findByIdWithTags(2L)).thenReturn(Optional.of(article));

        // Act
        documentIngestionService.ingestArticle(article);

        // Assert
        verify(vectorStore).delete(List.of("article-2"));
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void should_removeEmbedding_when_ingestArticleCalledWithNonExistentArticle() {
        // Arrange
        Article article = new Article();
        article.setId(99L);
        when(articleRepository.findByIdWithTags(99L)).thenReturn(Optional.empty());

        // Act
        documentIngestionService.ingestArticle(article);

        // Assert
        verify(vectorStore).delete(List.of("article-99"));
    }

    // ========== Ingest Project Tests ==========

    @Test
    void should_addDocumentToVectorStore_when_ingestProjectCalledWithValidProject() {
        // Arrange
        Project project = createProject(1L, "Portfolio", "A portfolio website",
                "Spring Boot, Angular", Set.of(createTag("Java")));
        when(projectRepository.findByIdWithTags(1L)).thenReturn(Optional.of(project));
        when(markdownService.renderToHtml("A portfolio website")).thenReturn("<p>A portfolio website</p>");

        // Act
        documentIngestionService.ingestProject(project);

        // Assert
        verify(vectorStore).add(documentsCaptor.capture());
        Document doc = documentsCaptor.getValue().get(0);
        assertThat(doc.getId()).isEqualTo("project-1");
        assertThat(doc.getMetadata().get("type")).isEqualTo("project");
        assertThat(doc.getMetadata().get("title")).isEqualTo("Portfolio");
        assertThat(doc.getText()).contains("Portfolio");
        assertThat(doc.getText()).contains("Tech Stack: Spring Boot, Angular");
    }

    @Test
    void should_removeEmbedding_when_ingestProjectCalledWithNonExistentProject() {
        // Arrange
        Project project = new Project();
        project.setId(99L);
        when(projectRepository.findByIdWithTags(99L)).thenReturn(Optional.empty());

        // Act
        documentIngestionService.ingestProject(project);

        // Assert
        verify(vectorStore).delete(List.of("project-99"));
    }

    // ========== Ingest Experience Tests ==========

    @Test
    void should_addDocumentToVectorStore_when_ingestExperienceCalledWithValidExperience() {
        // Arrange
        Experience experience = createExperience(1L, "Software Engineer", "Acme Corp",
                ExperienceType.WORK, LocalDate.of(2023, 1, 1), null, "Building microservices");
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(experience));

        // Act
        documentIngestionService.ingestExperience(experience);

        // Assert
        verify(vectorStore).add(documentsCaptor.capture());
        Document doc = documentsCaptor.getValue().get(0);
        assertThat(doc.getId()).isEqualTo("experience-1");
        assertThat(doc.getMetadata().get("type")).isEqualTo("experience");
        assertThat(doc.getMetadata().get("title")).isEqualTo("Software Engineer");
        assertThat(doc.getText()).contains("Software Engineer");
        assertThat(doc.getText()).contains("Acme Corp");
        assertThat(doc.getText()).contains("2023-01");
        assertThat(doc.getText()).contains("Present");
    }

    @Test
    void should_includeEndDate_when_ingestExperienceCalledWithCompletedExperience() {
        // Arrange
        Experience experience = createExperience(2L, "Intern", "Startup", ExperienceType.WORK,
                LocalDate.of(2022, 6, 1), LocalDate.of(2022, 12, 31), "Frontend development");
        when(experienceRepository.findById(2L)).thenReturn(Optional.of(experience));

        // Act
        documentIngestionService.ingestExperience(experience);

        // Assert
        verify(vectorStore).add(documentsCaptor.capture());
        String text = documentsCaptor.getValue().get(0).getText();
        assertThat(text).contains("2022-06");
        assertThat(text).contains("2022-12");
        assertThat(text).doesNotContain("Present");
    }

    @Test
    void should_removeEmbedding_when_ingestExperienceCalledWithNonExistentExperience() {
        // Arrange
        Experience experience = new Experience();
        experience.setId(99L);
        when(experienceRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        documentIngestionService.ingestExperience(experience);

        // Assert
        verify(vectorStore).delete(List.of("experience-99"));
    }

    // ========== Remove Tests ==========

    @Test
    void should_deleteFromVectorStore_when_removeArticleCalled() {
        // Act
        documentIngestionService.removeArticle(5L);

        // Assert
        verify(vectorStore).delete(List.of("article-5"));
    }

    @Test
    void should_deleteFromVectorStore_when_removeProjectCalled() {
        // Act
        documentIngestionService.removeProject(3L);

        // Assert
        verify(vectorStore).delete(List.of("project-3"));
    }

    @Test
    void should_deleteFromVectorStore_when_removeExperienceCalled() {
        // Act
        documentIngestionService.removeExperience(7L);

        // Assert
        verify(vectorStore).delete(List.of("experience-7"));
    }

    // ========== Ingest All Tests ==========

    @Test
    void should_ingestAllEntities_when_ingestAllCalled() {
        // Arrange
        Article article = createPublishedArticle(1L, "Article", "article", "Content", null, Set.of());
        Project project = createProject(1L, "Project", "Description", null, Set.of());
        Experience experience = createExperience(1L, "Role", "Company", ExperienceType.WORK,
                null, null, "Description text");

        when(articleRepository.findPublishedWithTags(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(article));
        when(projectRepository.findAllWithTags()).thenReturn(List.of(project));
        when(experienceRepository.findAll()).thenReturn(List.of(experience));
        when(markdownService.renderToHtml("Content")).thenReturn("<p>Content</p>");
        when(markdownService.renderToHtml("Description")).thenReturn("<p>Description</p>");

        // Act
        int count = documentIngestionService.ingestAll();

        // Assert
        assertThat(count).isEqualTo(3);
        verify(vectorStore).add(documentsCaptor.capture());
        List<Document> docs = documentsCaptor.getValue();
        assertThat(docs).hasSize(3);
    }

    @Test
    void should_returnZero_when_ingestAllCalledWithNoEntities() {
        // Arrange
        when(articleRepository.findPublishedWithTags(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(projectRepository.findAllWithTags()).thenReturn(List.of());
        when(experienceRepository.findAll()).thenReturn(List.of());

        // Act
        int count = documentIngestionService.ingestAll();

        // Assert
        assertThat(count).isEqualTo(0);
        verify(vectorStore, never()).add(anyList());
    }

    // ========== Error Handling Tests ==========

    @Test
    void should_notThrow_when_ingestArticleAndVectorStoreThrowsException() {
        // Arrange
        Article article = createPublishedArticle(1L, "Test", "test", "Content", null, Set.of());
        when(articleRepository.findByIdWithTags(1L)).thenReturn(Optional.of(article));
        when(markdownService.renderToHtml("Content")).thenReturn("<p>Content</p>");
        doThrow(new RuntimeException("ES down")).when(vectorStore).add(anyList());

        // Act -- should not throw
        documentIngestionService.ingestArticle(article);

        // Assert -- no exception propagated
    }

    @Test
    void should_notThrow_when_removeArticleAndVectorStoreThrowsException() {
        // Arrange
        doThrow(new RuntimeException("ES down")).when(vectorStore).delete(anyList());

        // Act -- should not throw
        documentIngestionService.removeArticle(1L);
    }

    // ========== Helper Methods ==========

    private Article createPublishedArticle(Long id, String title, String slug,
                                           String content, String excerpt, Set<Tag> tags) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setSlug(slug);
        article.setContent(content);
        article.setExcerpt(excerpt);
        article.setDraft(false);
        article.setPublishedAt(LocalDateTime.now().minusDays(1));
        article.setTags(tags);
        return article;
    }

    private Article createDraftArticle(Long id) {
        Article article = new Article();
        article.setId(id);
        article.setTitle("Draft");
        article.setSlug("draft");
        article.setContent("Draft content");
        article.setDraft(true);
        return article;
    }

    private Project createProject(Long id, String title, String description,
                                  String techStack, Set<Tag> tags) {
        Project project = new Project();
        project.setId(id);
        project.setTitle(title);
        project.setDescription(description);
        project.setTechStack(techStack);
        project.setTags(tags);
        return project;
    }

    private Experience createExperience(Long id, String role, String company,
                                        ExperienceType type, LocalDate startDate,
                                        LocalDate endDate, String description) {
        Experience experience = new Experience();
        experience.setId(id);
        experience.setRole(role);
        experience.setCompany(company);
        experience.setType(type);
        experience.setStartDate(startDate);
        experience.setEndDate(endDate);
        experience.setDescription(description);
        return experience;
    }

    private Tag createTag(String name) {
        Tag tag = new Tag();
        tag.setId((long) name.hashCode());
        tag.setName(name);
        tag.setColor("#007bff");
        return tag;
    }
}
