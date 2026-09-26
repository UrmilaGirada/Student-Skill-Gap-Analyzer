package com.skillgap.analyzer.exception;

/**
 * Thrown when a resource already exists - mapped to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
