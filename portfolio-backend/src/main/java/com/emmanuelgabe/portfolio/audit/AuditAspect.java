package com.emmanuelgabe.portfolio.audit;

import com.emmanuelgabe.portfolio.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Aspect for intercepting methods annotated with @Auditable.
 * Creates audit log entries for CRUD and business operations.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();

    /**
     * Intercepts methods annotated with @Auditable and creates audit log entries.
     *
     * @param joinPoint the join point
     * @param auditable the Auditable annotation
     * @return the method result
     * @throws Throwable if the method throws an exception
     */
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        log.debug("[AUDIT] Intercepting method - action={}, entityType={}",
                auditable.action(), auditable.entityType());

        AuditContext context = AuditContextHolder.getContext();
        Map<String, Object> oldValues = null;
        Long entityId = null;

        // Capture old values for UPDATE/DELETE if needed
        if (auditable.captureOldValues() && shouldCaptureOldValues(auditable.action())) {
            entityId = extractEntityId(joinPoint, auditable, null);
            if (entityId != null) {
                oldValues = auditService.captureEntityState(auditable.entityType(), entityId);
                log.debug("[AUDIT] Captured old values - entityType={}, entityId={}",
                        auditable.entityType(), entityId);
            }
        }

        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            success = false;
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                createAuditEntry(joinPoint, auditable, context, result, entityId,
                        oldValues, success, errorMessage);
            } catch (Exception e) {
                log.error("[AUDIT] Failed to create audit log - error={}", e.getMessage(), e);
            }
        }
    }

    /**
     * Creates the audit log entry after method execution.
     */
    private void createAuditEntry(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            AuditContext context,
            Object result,
            Long entityId,
            Map<String, Object> oldValues,
            boolean success,
            String errorMessage
    ) {
        // Extract entity ID from result if not already done
        if (entityId == null) {
            entityId = extractEntityId(joinPoint, auditable, result);
        }

        String entityName = extractEntityName(joinPoint, auditable, result);

        // Capture new values for CREATE/UPDATE
        Map<String, Object> newValues = null;
        if (success && shouldCaptureNewValues(auditable.action()) && result != null) {
            newValues = auditService.convertToMap(result);
        }

        // Create audit log
        auditService.createAuditLog(
                context,
                auditable.action(),
                auditable.entityType(),
                entityId,
                entityName,
                oldValues,
                newValues,
                success,
                errorMessage
        );

        log.info("[AUDIT] Audit log created - action={}, entityType={}, entityId={}, success={}",
                auditable.action(), auditable.entityType(), entityId, success);
    }

    /**
     * Determines if old values should be captured based on action type.
     */
    private boolean shouldCaptureOldValues(AuditAction action) {
        return action == AuditAction.UPDATE
                || action == AuditAction.DELETE
                || action == AuditAction.PUBLISH
                || action == AuditAction.UNPUBLISH
                || action == AuditAction.FEATURE
                || action == AuditAction.UNFEATURE
                || action == AuditAction.SET_CURRENT;
    }

    /**
     * Determines if new values should be captured based on action type.
     */
    private boolean shouldCaptureNewValues(AuditAction action) {
        return action == AuditAction.CREATE
                || action == AuditAction.UPDATE
                || action == AuditAction.PUBLISH
                || action == AuditAction.UNPUBLISH
                || action == AuditAction.FEATURE
                || action == AuditAction.UNFEATURE
                || action == AuditAction.SET_CURRENT;
    }

    /**
     * Extracts entity ID using SpEL expression.
     */
    private Long extractEntityId(ProceedingJoinPoint joinPoint, Auditable auditable, Object result) {
        if (auditable.entityIdExpression().isEmpty()) {
            return null;
        }
        return evaluateSpelExpression(joinPoint, auditable.entityIdExpression(), result, Long.class);
    }

    /**
     * Extracts entity name using SpEL expression.
     */
    private String extractEntityName(ProceedingJoinPoint joinPoint, Auditable auditable,
                                     Object result) {
        if (auditable.entityNameExpression().isEmpty()) {
            return null;
        }
        return evaluateSpelExpression(joinPoint, auditable.entityNameExpression(), result,
                String.class);
    }

    /**
     * Evaluates a SpEL expression against method parameters and result.
     *
     * @param joinPoint the join point
     * @param expression the SpEL expression
     * @param result the method result (for #result references)
     * @param targetType the expected result type
     * @return the evaluated value or null
     */
    private <T> T evaluateSpelExpression(ProceedingJoinPoint joinPoint, String expression,
                                         Object result, Class<T> targetType) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(),
                    method,
                    joinPoint.getArgs(),
                    parameterNameDiscoverer
            );
            context.setVariable("result", result);

            return spelParser.parseExpression(expression).getValue(context, targetType);
        } catch (Exception e) {
            log.debug("[AUDIT] Failed to evaluate SpEL expression - expression={}, error={}",
                    expression, e.getMessage());
            return null;
        }
    }
}
