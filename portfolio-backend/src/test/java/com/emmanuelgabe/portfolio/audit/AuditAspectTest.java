package com.emmanuelgabe.portfolio.audit;

import com.emmanuelgabe.portfolio.entity.AuditLog;
import com.emmanuelgabe.portfolio.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditAspect.
 * Tests AOP interception and SpEL expression evaluation.
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditAspect auditAspect;

    private ProceedingJoinPoint joinPoint;
    private MethodSignature methodSignature;
    private AuditContext testContext;

    @BeforeEach
    void setUp() {
        joinPoint = mock(ProceedingJoinPoint.class);
        methodSignature = mock(MethodSignature.class);

        testContext = AuditContext.builder()
                .userId(1L)
                .username("admin")
                .userRole("ADMIN")
                .ipAddress("127.0.0.1")
                .build();

        AuditContextHolder.setContext(testContext);

        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(auditService.createAuditLog(
                any(), any(), anyString(), any(), any(), any(), any(), anyBoolean(), any()
        )).thenReturn(new AuditLog());
    }

    @AfterEach
    void tearDown() {
        AuditContextHolder.clear();
    }

    // ========== Audit Interception Tests ==========

    @Test
    void should_createAuditLog_when_auditCalledOnSuccessfulMethod() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.CREATE, "Project", "#result.id", "#result.title", true);
        TestResult result = new TestResult(1L, "Test Project");

        when(joinPoint.proceed()).thenReturn(result);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(auditService.convertToMap(result)).thenReturn(Map.of("id", 1L, "title", "Test Project"));

        // Act
        Object returnValue = auditAspect.audit(joinPoint, auditable);

        // Assert
        assertThat(returnValue).isEqualTo(result);
        verify(auditService, times(1)).createAuditLog(
                eq(testContext),
                eq(AuditAction.CREATE),
                eq("Project"),
                eq(1L),
                eq("Test Project"),
                isNull(),
                any(),
                eq(true),
                isNull()
        );
    }

    @Test
    void should_captureOldValues_when_auditCalledOnUpdateAction() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.UPDATE, "Project", "#id", "#result.title", true);
        TestResult result = new TestResult(1L, "Updated Title");
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("title", "Old Title");

        when(joinPoint.proceed()).thenReturn(result);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(methodSignature.getMethod()).thenReturn(getUpdateMethod());
        when(auditService.captureEntityState("Project", 1L)).thenReturn(oldValues);
        when(auditService.convertToMap(result)).thenReturn(Map.of("id", 1L, "title", "Updated Title"));

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, times(1)).captureEntityState("Project", 1L);
        verify(auditService, times(1)).createAuditLog(
                any(),
                eq(AuditAction.UPDATE),
                eq("Project"),
                eq(1L),
                eq("Updated Title"),
                eq(oldValues),
                any(),
                eq(true),
                isNull()
        );
    }

    @Test
    void should_logFailure_when_auditCalledOnMethodThatThrowsException() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.CREATE, "Project", "#result.id", "#result.title", false);
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());

        // Act & Assert
        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        verify(auditService, times(1)).createAuditLog(
                any(),
                eq(AuditAction.CREATE),
                eq("Project"),
                any(),
                any(),
                any(),
                any(),
                eq(false),
                eq("Test exception")
        );
    }

    @Test
    void should_notCaptureOldValues_when_auditCalledOnCreateAction() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.CREATE, "Project", "#result.id", "#result.title", true);
        TestResult result = new TestResult(1L, "New Project");

        when(joinPoint.proceed()).thenReturn(result);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, never()).captureEntityState(anyString(), anyLong());
    }

    @Test
    void should_captureOldValues_when_auditCalledOnDeleteAction() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.DELETE, "Project", "#id", "", true);
        Map<String, Object> oldValues = Map.of("id", 1L, "title", "Deleted Project");

        when(joinPoint.proceed()).thenReturn(null);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(methodSignature.getMethod()).thenReturn(getDeleteMethod());
        when(auditService.captureEntityState("Project", 1L)).thenReturn(oldValues);

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, times(1)).captureEntityState("Project", 1L);
    }

    // ========== ShouldCaptureOldValues Tests ==========

    @Test
    void should_captureOldValues_when_actionIsUpdate() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.UPDATE, "Project", "#id", "", true);

        when(joinPoint.proceed()).thenReturn(new TestResult(1L, "Test"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(methodSignature.getMethod()).thenReturn(getUpdateMethod());

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, times(1)).captureEntityState(eq("Project"), eq(1L));
    }

    @Test
    void should_captureOldValues_when_actionIsPublish() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.PUBLISH, "Article", "#id", "", true);

        when(joinPoint.proceed()).thenReturn(new TestResult(1L, "Test"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(methodSignature.getMethod()).thenReturn(getUpdateMethod());

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, times(1)).captureEntityState(eq("Article"), eq(1L));
    }

    @Test
    void should_captureOldValues_when_actionIsUnpublish() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.UNPUBLISH, "Article", "#id", "", true);

        when(joinPoint.proceed()).thenReturn(new TestResult(1L, "Test"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(methodSignature.getMethod()).thenReturn(getUpdateMethod());

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, times(1)).captureEntityState(eq("Article"), eq(1L));
    }

    @Test
    void should_captureOldValues_when_actionIsSetCurrent() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.SET_CURRENT, "Cv", "#id", "", true);

        when(joinPoint.proceed()).thenReturn(new TestResult(1L, "Test"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(methodSignature.getMethod()).thenReturn(getUpdateMethod());

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, times(1)).captureEntityState(eq("Cv"), eq(1L));
    }

    @Test
    void should_notCaptureOldValues_when_captureOldValuesIsFalseEvenForUpdate() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.UPDATE, "Project", "#id", "", false);

        when(joinPoint.proceed()).thenReturn(new TestResult(1L, "Test"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(methodSignature.getMethod()).thenReturn(getUpdateMethod());

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, never()).captureEntityState(anyString(), anyLong());
    }

    @Test
    void should_handleNullContext_when_contextHolderReturnsNull() throws Throwable {
        // Arrange
        AuditContextHolder.clear();
        Auditable auditable = createAuditableAnnotation(
                AuditAction.CREATE, "Project", "#result.id", "", false);
        TestResult result = new TestResult(1L, "Test");

        when(joinPoint.proceed()).thenReturn(result);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, times(1)).createAuditLog(
                isNull(),
                eq(AuditAction.CREATE),
                eq("Project"),
                eq(1L),
                any(),
                any(),
                any(),
                eq(true),
                isNull()
        );
    }

    @Test
    void should_continueExecution_when_auditServiceThrowsException() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.CREATE, "Project", "#result.id", "", false);
        TestResult result = new TestResult(1L, "Test");

        when(joinPoint.proceed()).thenReturn(result);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        // Use lenient because the exception is caught internally and doesn't propagate
        lenient().when(auditService.createAuditLog(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // Act - should not throw, audit failure should not break the main flow
        Object returnValue = auditAspect.audit(joinPoint, auditable);

        // Assert
        assertThat(returnValue).isEqualTo(result);
    }

    @Test
    void should_handleEmptyEntityIdExpression_when_noExpressionProvided() throws Throwable {
        // Arrange
        Auditable auditable = createAuditableAnnotation(
                AuditAction.CREATE, "Project", "", "", false);
        TestResult result = new TestResult(1L, "Test");

        when(joinPoint.proceed()).thenReturn(result);
        // These are lenient because SpEL evaluation is skipped when expressions are empty
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
        lenient().when(methodSignature.getMethod()).thenReturn(getTestMethod());

        // Act
        auditAspect.audit(joinPoint, auditable);

        // Assert
        verify(auditService, times(1)).createAuditLog(
                any(),
                eq(AuditAction.CREATE),
                eq("Project"),
                isNull(),
                isNull(),
                any(),
                any(),
                eq(true),
                isNull()
        );
    }

    // ========== Helper Methods ==========

    private Auditable createAuditableAnnotation(
            AuditAction action,
            String entityType,
            String entityIdExpression,
            String entityNameExpression,
            boolean captureOldValues
    ) {
        return new Auditable() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Auditable.class;
            }

            @Override
            public AuditAction action() {
                return action;
            }

            @Override
            public String entityType() {
                return entityType;
            }

            @Override
            public String entityIdExpression() {
                return entityIdExpression;
            }

            @Override
            public String entityNameExpression() {
                return entityNameExpression;
            }

            @Override
            public boolean captureOldValues() {
                return captureOldValues;
            }
        };
    }

    private Method getTestMethod() throws NoSuchMethodException {
        return TestService.class.getMethod("createProject");
    }

    private Method getUpdateMethod() throws NoSuchMethodException {
        return TestService.class.getMethod("updateProject", Long.class);
    }

    private Method getDeleteMethod() throws NoSuchMethodException {
        return TestService.class.getMethod("deleteProject", Long.class);
    }

    /**
     * Test service class for method reflection.
     */
    public static class TestService {
        public TestResult createProject() {
            return new TestResult(1L, "Test");
        }

        public TestResult updateProject(Long id) {
            return new TestResult(id, "Updated");
        }

        public void deleteProject(Long id) {
            // Delete operation
        }
    }

    /**
     * Test result class for SpEL evaluation.
     */
    public static class TestResult {
        private final Long id;
        private final String title;

        TestResult(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }
}
