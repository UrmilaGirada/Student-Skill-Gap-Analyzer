package com.skillgap.analyzer.exception;

/**
 * Thrown when a document cannot be parsed at all (corrupted or unreadable file) - mapped to HTTP 422.
 */
public class ResumeExtractionException extends RuntimeException {

    public ResumeExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
