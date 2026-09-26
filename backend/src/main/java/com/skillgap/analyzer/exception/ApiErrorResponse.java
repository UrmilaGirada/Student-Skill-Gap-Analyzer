package com.skillgap.analyzer.exception;

/**
 * Error payload returned by the REST API - no stack traces, no internal details.
 */
public record ApiErrorResponse(int status, String message) {
}
