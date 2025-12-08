package com.emmanuelgabe.portfolio.graphql;

import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;

import com.emmanuelgabe.portfolio.graphql.resolver.query.ProjectQueryResolver;
import com.emmanuelgabe.portfolio.graphql.config.GraphQLConfig;

/**
 * GraphQL integration tests for Project queries.
 */
@GraphQlTest(ProjectQueryResolver.class)
@Import(GraphQLConfig.class)
class ProjectGraphQLTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private ProjectService projectService;

    // ========== projects Query Tests ==========

    @Test
    void should_returnProjects_when_projectsQueryExecuted() {
        // Arrange
        ProjectResponse project = createTestProject();
        when(projectService.getAllProjects()).thenReturn(List.of(project));

        // Act & Assert
        graphQlTester.document("""
                    query {
                        projects {
                            edges {
                                node {
                                    id
                                    title
                                    description
                                }
                            }
                            totalCount
                        }
                    }
                """)
                .execute()
                .path("projects.totalCount")
                .entity(Long.class)
                .isEqualTo(1L)
                .path("projects.edges[0].node.title")
                .entity(String.class)
                .isEqualTo("Test Project");
    }

    @Test
    void should_returnEmptyConnection_when_noProjectsExist() {
        // Arrange
        when(projectService.getAllProjects()).thenReturn(List.of());

        // Act & Assert
        graphQlTester.document("""
                    query {
                        projects {
                            edges {
                                node {
                                    id
                                }
                            }
                            totalCount
                        }
                    }
                """)
                .execute()
                .path("projects.totalCount")
                .entity(Long.class)
                .isEqualTo(0L)
                .path("projects.edges")
                .entityList(Object.class)
                .hasSize(0);
    }

    // ========== featuredProjects Query Tests ==========

    @Test
    void should_returnFeaturedProjects_when_featuredProjectsQueryExecuted() {
        // Arrange
        ProjectResponse project = createTestProject();
        project.setFeatured(true);
        when(projectService.getFeaturedProjects()).thenReturn(List.of(project));

        // Act & Assert
        graphQlTester.document("""
                    query {
                        featuredProjects {
                            id
                            title
                            featured
                        }
                    }
                """)
                .execute()
                .path("featuredProjects")
                .entityList(ProjectResponse.class)
                .hasSize(1)
                .path("featuredProjects[0].featured")
                .entity(Boolean.class)
                .isEqualTo(true);
    }

    // ========== project Query Tests ==========

    @Test
    void should_returnProject_when_projectQueryExecutedWithValidId() {
        // Arrange
        ProjectResponse project = createTestProject();
        when(projectService.getProjectById(1L)).thenReturn(project);

        // Act & Assert
        graphQlTester.document("""
                    query {
                        project(id: 1) {
                            id
                            title
                            description
                            techStack
                            tags {
                                id
                                name
                            }
                        }
                    }
                """)
                .execute()
                .path("project.id")
                .entity(Long.class)
                .isEqualTo(1L)
                .path("project.title")
                .entity(String.class)
                .isEqualTo("Test Project");
    }

    private ProjectResponse createTestProject() {
        ProjectResponse project = new ProjectResponse();
        project.setId(1L);
        project.setTitle("Test Project");
        project.setDescription("Test Description");
        project.setTechStack("Java, Spring Boot");
        project.setGithubUrl("https://github.com/test/project");
        project.setFeatured(false);
        project.setHasDetails(true);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        TagResponse tag = new TagResponse();
        tag.setId(1L);
        tag.setName("Java");
        tag.setColor("#FF5733");
        project.setTags(Set.of(tag));
        project.setImages(List.of());

        return project;
    }
}
