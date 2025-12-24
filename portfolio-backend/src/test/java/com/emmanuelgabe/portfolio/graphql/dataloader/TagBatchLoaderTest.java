package com.emmanuelgabe.portfolio.graphql.dataloader;

import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.mapper.TagMapper;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TagBatchLoader.
 */
@ExtendWith(MockitoExtension.class)
class TagBatchLoaderTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagBatchLoader tagBatchLoader;

    @BeforeEach
    void setUp() {
        lenient().when(tagMapper.toResponse(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
        });
    }

    // ========== loadTagsForProjects Tests ==========

    @Test
    void should_returnTagsByProjectId_when_loadTagsForProjectsCalled() throws Exception {
        // Arrange
        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setName("Java");
        tag1.setColor("#FF5733");

        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("Spring");
        tag2.setColor("#6DB33F");

        Project project1 = new Project();
        project1.setId(1L);
        project1.setTags(Set.of(tag1, tag2));

        Project project2 = new Project();
        project2.setId(2L);
        project2.setTags(Set.of(tag1));

        when(projectRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(project1, project2));

        // Act
        CompletionStage<Map<Long, List<TagResponse>>> result = tagBatchLoader.loadTagsForProjects(Set.of(1L, 2L));
        Map<Long, List<TagResponse>> tagsMap = result.toCompletableFuture().get();

        // Assert
        assertThat(tagsMap).hasSize(2);
        assertThat(tagsMap.get(1L)).hasSize(2);
        assertThat(tagsMap.get(2L)).hasSize(1);
    }

    @Test
    void should_returnEmptyListForMissingProjects_when_loadTagsForProjectsCalled() throws Exception {
        // Arrange
        when(projectRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of());

        // Act
        CompletionStage<Map<Long, List<TagResponse>>> result = tagBatchLoader.loadTagsForProjects(Set.of(1L, 2L));
        Map<Long, List<TagResponse>> tagsMap = result.toCompletableFuture().get();

        // Assert
        assertThat(tagsMap).hasSize(2);
        assertThat(tagsMap.get(1L)).isEmpty();
        assertThat(tagsMap.get(2L)).isEmpty();
    }

    // ========== loadTagsForArticles Tests ==========

    @Test
    void should_returnTagsByArticleId_when_loadTagsForArticlesCalled() throws Exception {
        // Arrange
        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setName("GraphQL");
        tag1.setColor("#E535AB");

        Article article1 = new Article();
        article1.setId(1L);
        article1.setTags(Set.of(tag1));

        when(articleRepository.findAllById(Set.of(1L))).thenReturn(List.of(article1));

        // Act
        CompletionStage<Map<Long, List<TagResponse>>> result = tagBatchLoader.loadTagsForArticles(Set.of(1L));
        Map<Long, List<TagResponse>> tagsMap = result.toCompletableFuture().get();

        // Assert
        assertThat(tagsMap).hasSize(1);
        assertThat(tagsMap.get(1L)).hasSize(1);
        assertThat(tagsMap.get(1L).get(0).getName()).isEqualTo("GraphQL");
    }

    @Test
    void should_returnEmptyListForMissingArticles_when_loadTagsForArticlesCalled() throws Exception {
        // Arrange
        when(articleRepository.findAllById(Set.of(1L))).thenReturn(List.of());

        // Act
        CompletionStage<Map<Long, List<TagResponse>>> result = tagBatchLoader.loadTagsForArticles(Set.of(1L));
        Map<Long, List<TagResponse>> tagsMap = result.toCompletableFuture().get();

        // Assert
        assertThat(tagsMap).hasSize(1);
        assertThat(tagsMap.get(1L)).isEmpty();
    }
}
