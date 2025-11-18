package com.emmanuelgabe.portfolio.exception;

/**
 * Exception thrown when JWT or refresh token is invalid
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
