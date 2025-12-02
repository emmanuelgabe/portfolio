package com.emmanuelgabe.portfolio.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
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

    // ========== ResourceNotFoundException Tests ==========

    @Test
    void should_return404_when_handleResourceNotFoundExceptionCalledWithLongId() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("Project", "id", 999L);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("Project not found with id : '999'");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void should_return404_when_handleResourceNotFoundExceptionCalledWithStringValue() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("Tag", "name", "Java");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("Tag not found with name : 'Java'");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void should_containCorrectDetails_when_resourceNotFoundExceptionMessageGenerated() {
        // Arrange
        ResourceNotFoundException exception1 = new ResourceNotFoundException("Project", "id", 1L);
        ResourceNotFoundException exception2 = new ResourceNotFoundException("Skill", "name", "Java");
        ResourceNotFoundException exception3 = new ResourceNotFoundException("Tag", "color", "#FF5733");

        // Act & Assert
        assertThat(exception1.getMessage()).contains("Project").contains("id").contains("1");
        assertThat(exception2.getMessage()).contains("Skill").contains("name").contains("Java");
        assertThat(exception3.getMessage()).contains("Tag").contains("color").contains("#FF5733");
    }

    // ========== InvalidCredentialsException Tests ==========

    @Test
    void should_return401_when_handleInvalidCredentialsExceptionCalled() {
        // Arrange
        InvalidCredentialsException exception = new InvalidCredentialsException("Invalid username or password");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleInvalidCredentialsException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ========== InvalidTokenException Tests ==========

    @Test
    void should_return401_when_handleInvalidTokenExceptionCalled() {
        // Arrange
        InvalidTokenException exception = new InvalidTokenException("Token is invalid");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleInvalidTokenException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Token is invalid");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ========== TokenExpiredException Tests ==========

    @Test
    void should_return401_when_handleTokenExpiredExceptionCalled() {
        // Arrange
        TokenExpiredException exception = new TokenExpiredException("Token has expired");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleTokenExpiredException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Token has expired");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ========== JWT Exception Tests ==========

    @Test
    void should_return401_when_handleSignatureExceptionCalled() {
        // Arrange
        SignatureException exception = mock(SignatureException.class);
        when(exception.getMessage()).thenReturn("JWT signature does not match");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleSignatureException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid JWT signature");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void should_return401_when_handleExpiredJwtExceptionCalled() {
        // Arrange
        ExpiredJwtException exception = mock(ExpiredJwtException.class);
        when(exception.getMessage()).thenReturn("JWT token expired at 2024-01-01");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleExpiredJwtException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("JWT token has expired");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void should_return401_when_handleMalformedJwtExceptionCalled() {
        // Arrange
        MalformedJwtException exception = new MalformedJwtException("JWT string is malformed");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleMalformedJwtException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid JWT token");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void should_return401_when_handleUnsupportedJwtExceptionCalled() {
        // Arrange
        UnsupportedJwtException exception = new UnsupportedJwtException("JWT algorithm not supported");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleUnsupportedJwtException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Unsupported JWT token");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ========== Spring Security Exception Tests ==========

    @Test
    void should_return401_when_handleAuthenticationExceptionCalled() {
        // Arrange
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleAuthenticationException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).contains("Authentication failed");
        assertThat(response.getBody().getMessage()).contains("Bad credentials");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void should_return403_when_handleAccessDeniedExceptionCalled() {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleAccessDeniedException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).contains("Access denied");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ========== IllegalArgumentException Tests ==========

    @Test
    void should_return400_when_handleIllegalArgumentExceptionCalled() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleIllegalArgumentException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid parameter value");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ========== FileStorageException Tests ==========

    @Test
    void should_return400_when_handleFileStorageExceptionCalledWithValidationError() {
        // Arrange
        FileStorageException exception = new FileStorageException("File type not allowed: exe");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleFileStorageException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("File type not allowed");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void should_return400_when_handleFileStorageExceptionCalledWithSizeExceeded() {
        // Arrange
        FileStorageException exception = new FileStorageException("File size exceeds maximum allowed");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleFileStorageException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("exceeds maximum");
    }

    @Test
    void should_return400_when_handleFileStorageExceptionCalledWithEmptyFile() {
        // Arrange
        FileStorageException exception = new FileStorageException("File is empty");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleFileStorageException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("empty");
    }

    @Test
    void should_return400_when_handleFileStorageExceptionCalledWithInvalidFileName() {
        // Arrange
        FileStorageException exception = new FileStorageException("Invalid file name: ../etc/passwd");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleFileStorageException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("Invalid file name");
    }

    @Test
    void should_return400_when_handleFileStorageExceptionCalledWithNotValidFile() {
        // Arrange
        FileStorageException exception = new FileStorageException("File is not a valid PDF");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleFileStorageException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("not a valid");
    }

    @Test
    void should_return500_when_handleFileStorageExceptionCalledWithStorageError() {
        // Arrange
        FileStorageException exception = new FileStorageException("Could not store file to disk");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleFileStorageException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).contains("Could not store file");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ========== EmailException Tests ==========

    @Test
    void should_return500_when_handleEmailExceptionCalled() {
        // Arrange
        EmailException exception = new EmailException("SMTP server connection failed");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleEmailException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Failed to send email. Please try again later.");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ========== Validation Exception Tests ==========

    @Test
    void should_return400WithErrors_when_handleValidationExceptionsCalledWithMultipleErrors() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = mock(MethodParameter.class);

        FieldError fieldError1 = new FieldError("createProjectRequest", "title", "Title is required");
        FieldError fieldError2 = new FieldError("createProjectRequest", "description",
                "Description must be at least 10 characters");

        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getErrors()).hasSize(2);
        assertThat(response.getBody().getErrors()).containsEntry("title", "Title is required");
        assertThat(response.getBody().getErrors()).containsEntry("description",
                "Description must be at least 10 characters");
    }

    @Test
    void should_return400_when_handleValidationExceptionsCalledWithSingleError() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = mock(MethodParameter.class);

        FieldError fieldError = new FieldError("createSkillRequest", "level", "Level must be between 0 and 100");

        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors()).containsEntry("level", "Level must be between 0 and 100");
    }

    @Test
    void should_includeAllErrors_when_handleValidationExceptionsCalledWithThreeErrors() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = mock(MethodParameter.class);

        FieldError error1 = new FieldError("request", "name", "Name is required");
        FieldError error2 = new FieldError("request", "color", "Color must be a valid hex code");
        FieldError error3 = new FieldError("request", "category", "Category is required");

        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(error1, error2, error3));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).hasSize(3);
        assertThat(response.getBody().getErrors()).containsKeys("name", "color", "category");
    }

    @Test
    void should_setTimestamp_when_handleValidationExceptionsResponseCreated() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        MethodParameter methodParameter = mock(MethodParameter.class);
        FieldError fieldError = new FieldError("request", "field", "error");
        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }

    // ========== Global Exception Tests ==========

    @Test
    void should_return500_when_handleGlobalExceptionCalledWithRuntimeException() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected database error");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleGlobalException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).contains("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).contains("Unexpected database error");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void should_return500_when_handleGlobalExceptionCalledWithNullPointerException() {
        // Arrange
        Exception exception = new NullPointerException("Null value encountered");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleGlobalException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).contains("Null value encountered");
    }

    @Test
    void should_setTimestamp_when_handleGlobalExceptionResponseCreated() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("Project", "id", 1L);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }

    // ========== Response Structure Tests ==========

    @Test
    void should_haveCorrectStructure_when_errorResponseCreated() {
        // Arrange & Act
        GlobalExceptionHandler.ErrorResponse errorResponse =
                new GlobalExceptionHandler.ErrorResponse(404, "Not found", null);

        // Assert
        assertThat(errorResponse.getStatus()).isEqualTo(404);
        assertThat(errorResponse.getMessage()).isEqualTo("Not found");
    }

    @Test
    void should_extendErrorResponse_when_validationErrorResponseCreated() {
        // Arrange
        Map<String, String> errors = Map.of("field1", "error1", "field2", "error2");

        // Act
        GlobalExceptionHandler.ValidationErrorResponse validationErrorResponse =
                new GlobalExceptionHandler.ValidationErrorResponse(400, "Validation failed", null, errors);

        // Assert
        assertThat(validationErrorResponse.getStatus()).isEqualTo(400);
        assertThat(validationErrorResponse.getMessage()).isEqualTo("Validation failed");
        assertThat(validationErrorResponse.getErrors()).hasSize(2);
        assertThat(validationErrorResponse.getErrors()).containsEntry("field1", "error1");
        assertThat(validationErrorResponse.getErrors()).containsEntry("field2", "error2");
    }

    @Test
    void should_work_when_validationErrorResponseCreatedWithEmptyErrors() {
        // Arrange
        Map<String, String> errors = Map.of();

        // Act
        GlobalExceptionHandler.ValidationErrorResponse validationErrorResponse =
                new GlobalExceptionHandler.ValidationErrorResponse(400, "Validation failed", null, errors);

        // Assert
        assertThat(validationErrorResponse.getErrors()).isEmpty();
    }

    @Test
    void should_returnDefensiveCopy_when_getErrorsCalledOnValidationErrorResponse() {
        // Arrange
        Map<String, String> errors = Map.of("field", "error");
        GlobalExceptionHandler.ValidationErrorResponse response =
                new GlobalExceptionHandler.ValidationErrorResponse(400, "Validation failed", null, errors);

        // Act
        Map<String, String> retrievedErrors = response.getErrors();

        // Assert
        assertThat(retrievedErrors).isNotSameAs(errors);
        assertThat(retrievedErrors).containsEntry("field", "error");
    }
}
