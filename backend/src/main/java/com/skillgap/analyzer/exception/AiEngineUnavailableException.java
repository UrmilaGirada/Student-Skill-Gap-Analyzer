package com.skillgap.analyzer.exception;

/**
 * Thrown when the Python AI engine cannot be reached (not running, connection refused or timed
 * out) - mapped to HTTP 503.
 */
public class AiEngineUnavailableException extends RuntimeException {

    public AiEngineUnavailableException(String message) {
        super(message);
    }

    public AiEngineUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
