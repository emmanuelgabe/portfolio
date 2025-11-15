package com.emmanuelgabe.portfolio.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleResourceNotFoundException_ShouldReturn404() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Project", "id", 999L);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("Project not found with id : '999'");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleResourceNotFoundException_WithStringValue_ShouldReturn404() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Tag", "name", "Java");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("Tag not found with name : 'Java'");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleValidationExceptions_ShouldReturn400WithErrors() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = mock(MethodParameter.class);

        FieldError fieldError1 = new FieldError("createProjectRequest", "title", "Title is required");
        FieldError fieldError2 = new FieldError("createProjectRequest", "description", "Description must be at least 10 characters");
        List<FieldError> fieldErrors = Arrays.asList(fieldError1, fieldError2);

        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getErrors()).hasSize(2);
        assertThat(response.getBody().getErrors()).containsEntry("title", "Title is required");
        assertThat(response.getBody().getErrors()).containsEntry("description", "Description must be at least 10 characters");
    }

    @Test
    void handleValidationExceptions_WithSingleError_ShouldReturn400() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = mock(MethodParameter.class);

        FieldError fieldError = new FieldError("createSkillRequest", "level", "Level must be between 0 and 100");

        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors()).containsEntry("level", "Level must be between 0 and 100");
    }

    @Test
    void handleValidationExceptions_WithMultipleFieldsErrors_ShouldIncludeAllErrors() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = mock(MethodParameter.class);

        FieldError error1 = new FieldError("request", "name", "Name is required");
        FieldError error2 = new FieldError("request", "color", "Color must be a valid hex code");
        FieldError error3 = new FieldError("request", "category", "Category is required");

        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(error1, error2, error3));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).hasSize(3);
        assertThat(response.getBody().getErrors()).containsKeys("name", "color", "category");
    }

    @Test
    void handleGlobalException_ShouldReturn500() {
        // Given
        Exception exception = new RuntimeException("Unexpected database error");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleGlobalException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).contains("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).contains("Unexpected database error");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleGlobalException_WithNullPointerException_ShouldReturn500() {
        // Given
        Exception exception = new NullPointerException("Null value encountered");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleGlobalException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).contains("Null value encountered");
    }

    @Test
    void handleGlobalException_WithIllegalArgumentException_ShouldReturn500() {
        // Given
        Exception exception = new IllegalArgumentException("Invalid argument provided");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleGlobalException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).contains("Invalid argument provided");
    }

    @Test
    void errorResponse_ShouldHaveCorrectStructure() {
        // Given & Act
        GlobalExceptionHandler.ErrorResponse errorResponse =
                new GlobalExceptionHandler.ErrorResponse(404, "Not found", null);

        // Then
        assertThat(errorResponse.getStatus()).isEqualTo(404);
        assertThat(errorResponse.getMessage()).isEqualTo("Not found");
    }

    @Test
    void validationErrorResponse_ShouldExtendErrorResponse() {
        // Given
        Map<String, String> errors = Map.of("field1", "error1", "field2", "error2");

        // When
        GlobalExceptionHandler.ValidationErrorResponse validationErrorResponse =
                new GlobalExceptionHandler.ValidationErrorResponse(400, "Validation failed", null, errors);

        // Then
        assertThat(validationErrorResponse.getStatus()).isEqualTo(400);
        assertThat(validationErrorResponse.getMessage()).isEqualTo("Validation failed");
        assertThat(validationErrorResponse.getErrors()).hasSize(2);
        assertThat(validationErrorResponse.getErrors()).containsEntry("field1", "error1");
        assertThat(validationErrorResponse.getErrors()).containsEntry("field2", "error2");
    }

    @Test
    void validationErrorResponse_WithEmptyErrors_ShouldWork() {
        // Given
        Map<String, String> errors = Map.of();

        // When
        GlobalExceptionHandler.ValidationErrorResponse validationErrorResponse =
                new GlobalExceptionHandler.ValidationErrorResponse(400, "Validation failed", null, errors);

        // Then
        assertThat(validationErrorResponse.getErrors()).isEmpty();
    }

    @Test
    void testResourceNotFoundExceptionMessage() {
        // Given
        ResourceNotFoundException exception1 = new ResourceNotFoundException("Project", "id", 1L);
        ResourceNotFoundException exception2 = new ResourceNotFoundException("Skill", "name", "Java");
        ResourceNotFoundException exception3 = new ResourceNotFoundException("Tag", "color", "#FF5733");

        // When & Then
        assertThat(exception1.getMessage()).contains("Project").contains("id").contains("1");
        assertThat(exception2.getMessage()).contains("Skill").contains("name").contains("Java");
        assertThat(exception3.getMessage()).contains("Tag").contains("color").contains("#FF5733");
    }

    @Test
    void testTimestampIsSetInErrorResponse() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Project", "id", 1L);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }

    @Test
    void testTimestampIsSetInValidationErrorResponse() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = mock(MethodParameter.class);
        FieldError fieldError = new FieldError("request", "field", "error");
        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }
}
