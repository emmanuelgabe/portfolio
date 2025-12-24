package com.emmanuelgabe.portfolio.graphql;

import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.graphql.config.GraphQLConfig;
import com.emmanuelgabe.portfolio.graphql.exception.GraphQLExceptionHandler;
import com.emmanuelgabe.portfolio.graphql.resolver.query.ProjectQueryResolver;
import com.emmanuelgabe.portfolio.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.mockito.Mockito.when;

/**
 * GraphQL integration tests for error handling.
 */
@GraphQlTest(ProjectQueryResolver.class)
@Import({GraphQLConfig.class, GraphQLExceptionHandler.class})
class GraphQLErrorHandlingTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private ProjectService projectService;

    // ========== NOT_FOUND Error Tests ==========

    @Test
    void should_returnNotFoundError_when_projectNotExists() {
        // Arrange
        when(projectService.getProjectById(999L))
                .thenThrow(new ResourceNotFoundException("Project", "id", 999L));

        // Act & Assert
        graphQlTester.document("""
                    query {
                        project(id: 999) {
                            id
                            title
                        }
                    }
                """)
                .execute()
                .errors()
                .expect(error -> error.getMessage().contains("Project"))
                .expect(error -> error.getMessage().contains("999"));
    }

    // ========== Validation Error Tests ==========

    @Test
    void should_returnValidationError_when_invalidArgument() {
        // Arrange
        when(projectService.getProjectById(-1L))
                .thenThrow(new IllegalArgumentException("ID must be positive"));

        // Act & Assert
        graphQlTester.document("""
                    query {
                        project(id: -1) {
                            id
                            title
                        }
                    }
                """)
                .execute()
                .errors()
                .expect(error -> error.getMessage().contains("ID must be positive"));
    }

    // ========== Internal Error Tests ==========

    @Test
    void should_returnInternalError_when_unexpectedExceptionOccurs() {
        // Arrange
        when(projectService.getProjectById(1L))
                .thenThrow(new RuntimeException("Unexpected database error"));

        // Act & Assert
        graphQlTester.document("""
                    query {
                        project(id: 1) {
                            id
                            title
                        }
                    }
                """)
                .execute()
                .errors()
                .expect(error -> error.getMessage() != null);
    }
}
