package com.emmanuelgabe.portfolio.exception;

/**
 * Exception thrown when audit export operations fail
 */
public class AuditExportException extends RuntimeException {

    public AuditExportException(String message) {
        super(message);
    }

    public AuditExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
