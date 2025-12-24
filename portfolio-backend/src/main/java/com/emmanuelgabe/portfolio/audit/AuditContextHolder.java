package com.emmanuelgabe.portfolio.audit;

/**
 * ThreadLocal holder for AuditContext.
 * Allows access to audit context from any point in the request lifecycle.
 */
public final class AuditContextHolder {

    private static final ThreadLocal<AuditContext> CONTEXT = new ThreadLocal<>();

    private AuditContextHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Set the audit context for the current thread.
     *
     * @param context the audit context to set
     */
    public static void setContext(AuditContext context) {
        CONTEXT.set(context);
    }

    /**
     * Get the audit context for the current thread.
     *
     * @return the current audit context, or null if not set
     */
    public static AuditContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Clear the audit context for the current thread.
     * Should be called at the end of each request to prevent memory leaks.
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
