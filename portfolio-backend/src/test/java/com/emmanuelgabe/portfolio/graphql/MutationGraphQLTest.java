package com.emmanuelgabe.portfolio.graphql;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;
import com.emmanuelgabe.portfolio.graphql.config.GraphQLConfig;
import com.emmanuelgabe.portfolio.graphql.mapper.GraphQLInputMapper;
import com.emmanuelgabe.portfolio.graphql.resolver.mutation.ContactMutationResolver;
import com.emmanuelgabe.portfolio.graphql.resolver.mutation.ProjectMutationResolver;
import com.emmanuelgabe.portfolio.graphql.resolver.mutation.TagMutationResolver;
import com.emmanuelgabe.portfolio.service.ContactService;
import com.emmanuelgabe.portfolio.service.ProjectService;
import com.emmanuelgabe.portfolio.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * GraphQL integration tests for Mutations.
 */
@GraphQlTest({ProjectMutationResolver.class, TagMutationResolver.class, ContactMutationResolver.class})
@Import(GraphQLConfig.class)
class MutationGraphQLTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private TagService tagService;

    @MockBean
    private ContactService contactService;

    @MockBean
    private GraphQLInputMapper inputMapper;

    @BeforeEach
    void setUp() {
        // Configure inputMapper to pass through by creating proper request objects
        lenient().when(inputMapper.toCreateProjectRequest(any())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0);
            return new CreateProjectRequest();
        });
        lenient().when(inputMapper.toCreateTagRequest(any())).thenAnswer(invocation -> {
            return new CreateTagRequest();
        });
        lenient().when(inputMapper.toUpdateTagRequest(any())).thenAnswer(invocation -> {
            return new UpdateTagRequest();
        });
        lenient().when(inputMapper.toContactRequest(any())).thenAnswer(invocation -> {
            return new ContactRequest();
        });
    }

    // ========== Contact Mutation Tests (Public) ==========

    @Test
    void should_sendContactMessage_when_validInput() {
        // Arrange
        ContactResponse response = new ContactResponse();
        response.setSuccess(true);
        response.setMessage("Message sent successfully");

        when(contactService.sendContactEmail(any())).thenReturn(response);

        // Act & Assert
        graphQlTester.document("""
                    mutation {
                        sendContactMessage(input: {
                            name: "John Doe",
                            email: "john@example.com",
                            subject: "Test Subject",
                            message: "Test message content"
                        }) {
                            success
                            message
                        }
                    }
                """)
                .execute()
                .path("sendContactMessage.success")
                .entity(Boolean.class)
                .isEqualTo(true)
                .path("sendContactMessage.message")
                .entity(String.class)
                .isEqualTo("Message sent successfully");
    }

    // ========== Project Mutation Tests (Admin Required) ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_createProject_when_adminUserWithValidInput() {
        // Arrange
        ProjectResponse response = createTestProjectResponse();
        when(projectService.createProject(any())).thenReturn(response);

        // Act & Assert
        graphQlTester.document("""
                    mutation {
                        createProject(input: {
                            title: "New Project",
                            description: "Project description",
                            techStack: "Java, Spring Boot"
                        }) {
                            id
                            title
                            description
                        }
                    }
                """)
                .execute()
                .path("createProject.id")
                .entity(Long.class)
                .isEqualTo(1L)
                .path("createProject.title")
                .entity(String.class)
                .isEqualTo("Test Project");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_deleteProject_when_adminUserWithValidId() {
        // Arrange - no return value needed for delete

        // Act & Assert
        graphQlTester.document("""
                    mutation {
                        deleteProject(id: 1)
                    }
                """)
                .execute()
                .path("deleteProject")
                .entity(Boolean.class)
                .isEqualTo(true);
    }

    // ========== Tag Mutation Tests (Admin Required) ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_createTag_when_adminUserWithValidInput() {
        // Arrange
        TagResponse response = new TagResponse();
        response.setId(1L);
        response.setName("NewTag");
        response.setColor("#FF0000");

        when(tagService.createTag(any())).thenReturn(response);

        // Act & Assert
        graphQlTester.document("""
                    mutation {
                        createTag(input: {
                            name: "NewTag",
                            color: "#FF0000"
                        }) {
                            id
                            name
                            color
                        }
                    }
                """)
                .execute()
                .path("createTag.id")
                .entity(Long.class)
                .isEqualTo(1L)
                .path("createTag.name")
                .entity(String.class)
                .isEqualTo("NewTag");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_updateTag_when_adminUserWithValidInput() {
        // Arrange
        TagResponse response = new TagResponse();
        response.setId(1L);
        response.setName("UpdatedTag");
        response.setColor("#00FF00");

        when(tagService.updateTag(any(), any())).thenReturn(response);

        // Act & Assert
        graphQlTester.document("""
                    mutation {
                        updateTag(id: 1, input: {
                            name: "UpdatedTag",
                            color: "#00FF00"
                        }) {
                            id
                            name
                            color
                        }
                    }
                """)
                .execute()
                .path("updateTag.name")
                .entity(String.class)
                .isEqualTo("UpdatedTag");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_deleteTag_when_adminUserWithValidId() {
        // Act & Assert
        graphQlTester.document("""
                    mutation {
                        deleteTag(id: 1)
                    }
                """)
                .execute()
                .path("deleteTag")
                .entity(Boolean.class)
                .isEqualTo(true);
    }

    private ProjectResponse createTestProjectResponse() {
        ProjectResponse response = new ProjectResponse();
        response.setId(1L);
        response.setTitle("Test Project");
        response.setDescription("Test Description");
        response.setTechStack("Java, Spring Boot");
        response.setFeatured(false);
        response.setHasDetails(true);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        response.setTags(new HashSet<>());
        response.setImages(List.of());
        return response;
    }
}
