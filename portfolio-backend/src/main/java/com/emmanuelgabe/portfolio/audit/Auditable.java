package com.emmanuelgabe.portfolio.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for audit logging.
 * The AuditAspect will intercept calls to annotated methods
 * and create audit log entries.
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;Auditable(action = AuditAction.CREATE, entityType = "Project",
 *            entityIdExpression = "#result.id", entityNameExpression = "#result.title")
 * public ProjectResponse createProject(CreateProjectRequest request) {
 *     // implementation
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The audit action type (CREATE, UPDATE, DELETE, PUBLISH, etc.)
     */
    AuditAction action();

    /**
     * The entity type being audited (e.g., "Project", "Article", "Skill")
     */
    String entityType();

    /**
     * SpEL expression to extract entity ID from method arguments or return value.
     * <ul>
     *   <li>{@code #id} - from method parameter named "id"</li>
     *   <li>{@code #request.id} - from request object</li>
     *   <li>{@code #result.id} - from return value (after method execution)</li>
     * </ul>
     */
    String entityIdExpression() default "";

    /**
     * SpEL expression to extract entity name for human-readable display.
     * <ul>
     *   <li>{@code #request.title} - from request object</li>
     *   <li>{@code #result.title} - from return value</li>
     *   <li>{@code #result.name} - from return value</li>
     * </ul>
     */
    String entityNameExpression() default "";

    /**
     * Whether to capture old values for UPDATE/DELETE actions.
     * When true, the entity will be fetched before modification to capture its state.
     */
    boolean captureOldValues() default true;
}
