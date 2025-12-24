package com.emmanuelgabe.portfolio.graphql.exception;

import com.emmanuelgabe.portfolio.exception.FileStorageException;
import com.emmanuelgabe.portfolio.exception.InvalidCredentialsException;
import com.emmanuelgabe.portfolio.exception.InvalidTokenException;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.exception.TokenExpiredException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/**
 * GraphQL Exception Handler.
 * Maps domain exceptions to GraphQL errors with appropriate error types.
 */
@Slf4j
@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        // Resource not found
        if (ex instanceof ResourceNotFoundException) {
            log.warn("[GRAPHQL_ERROR] Resource not found - message={}", ex.getMessage());
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.NOT_FOUND)
                    .message(ex.getMessage())
                    .build();
        }

        // Access denied
        if (ex instanceof AccessDeniedException) {
            log.warn("[GRAPHQL_ERROR] Access denied - path={}", env.getExecutionStepInfo().getPath());
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.FORBIDDEN)
                    .message("Access denied. Admin role required.")
                    .build();
        }

        // Authentication errors
        if (ex instanceof InvalidCredentialsException || ex instanceof BadCredentialsException) {
            log.warn("[GRAPHQL_ERROR] Invalid credentials");
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message("Invalid credentials")
                    .build();
        }

        if (ex instanceof InvalidTokenException) {
            log.warn("[GRAPHQL_ERROR] Invalid token - message={}", ex.getMessage());
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message(ex.getMessage())
                    .build();
        }

        if (ex instanceof TokenExpiredException) {
            log.warn("[GRAPHQL_ERROR] Token expired");
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message("Token has expired")
                    .build();
        }

        // File storage errors
        if (ex instanceof FileStorageException) {
            log.error("[GRAPHQL_ERROR] File storage error - message={}", ex.getMessage());
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.INTERNAL_ERROR)
                    .message("File operation failed: " + ex.getMessage())
                    .build();
        }

        // Validation errors
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            log.warn("[GRAPHQL_ERROR] Validation error - message={}", ex.getMessage());
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .build();
        }

        // Unexpected errors
        log.error("[GRAPHQL_ERROR] Unexpected error - type={}, message={}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("An unexpected error occurred")
                .build();
    }
}
