package com.emmanuelgabe.portfolio.exception;

/**
 * Exception thrown when file validation fails.
 * This includes: empty files, invalid file types, size limits exceeded,
 * invalid file names, and invalid file content.
 *
 * Results in HTTP 400 Bad Request (client error).
 */
public class FileValidationException extends FileStorageException {

    public FileValidationException(String message) {
        super(message);
    }

    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
