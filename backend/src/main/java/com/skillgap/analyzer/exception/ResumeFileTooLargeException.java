package com.skillgap.analyzer.exception;

/**
 * Thrown when an uploaded resume exceeds the configured size limit - mapped to HTTP 413.
 */
public class ResumeFileTooLargeException extends RuntimeException {

    public ResumeFileTooLargeException(String message) {
        super(message);
    }
}
