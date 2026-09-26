package com.skillgap.analyzer.exception;

/**
 * Thrown when a document contains no extractable text (for example a scanned image-only PDF).
 * Mapped to HTTP 422. No OCR is attempted in this phase.
 */
public class NoExtractableTextException extends RuntimeException {

    public NoExtractableTextException(String message) {
        super(message);
    }
}
