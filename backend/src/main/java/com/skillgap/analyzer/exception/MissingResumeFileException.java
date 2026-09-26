package com.skillgap.analyzer.exception;

/**
 * Thrown when an upload is missing its file part or the file is empty - mapped to HTTP 400.
 */
public class MissingResumeFileException extends RuntimeException {

    public MissingResumeFileException(String message) {
        super(message);
    }
}
