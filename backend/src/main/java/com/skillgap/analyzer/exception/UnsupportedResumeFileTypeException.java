package com.skillgap.analyzer.exception;

/**
 * Thrown when a resume upload is not a supported document type - mapped to HTTP 415.
 */
public class UnsupportedResumeFileTypeException extends RuntimeException {

    public UnsupportedResumeFileTypeException(String message) {
        super(message);
    }
}
