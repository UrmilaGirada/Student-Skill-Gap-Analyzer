package com.skillgap.analyzer.exception;

/**
 * Thrown when the Python AI engine answered with an error status or an unexpected payload -
 * mapped to HTTP 502. Internal details of the Python process are never exposed to the client.
 */
public class AiEngineBadResponseException extends RuntimeException {

    public AiEngineBadResponseException(String message) {
        super(message);
    }

    public AiEngineBadResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
