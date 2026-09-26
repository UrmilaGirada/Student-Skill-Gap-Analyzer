package com.skillgap.analyzer.exception;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Translates application exceptions into clean JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    /** Bean validation failures on request bodies (missing/blank/out-of-range fields). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** Unreadable payloads: malformed JSON or an unknown enum value. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Malformed or invalid request body");
    }

    /** Last line of defence: a database constraint (for example a duplicate) was violated. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "Request violates a database constraint");
    }

    // ---------------------------------------------------------------------
    // Phase 4: resume upload
    // ---------------------------------------------------------------------

    /** The multipart request had no "file" part at all. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException exception) {
        return error(HttpStatus.BAD_REQUEST, "Resume file is required");
    }

    @ExceptionHandler(MissingResumeFileException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingResumeFile(MissingResumeFileException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UnsupportedResumeFileTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedResumeType(UnsupportedResumeFileTypeException exception) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage());
    }

    /** Raised by Spring/Tomcat itself when the configured multipart limits are exceeded. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Resume file exceeds the 10 MB limit");
    }

    @ExceptionHandler(ResumeFileTooLargeException.class)
    public ResponseEntity<ApiErrorResponse> handleResumeTooLarge(ResumeFileTooLargeException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage());
    }

    @ExceptionHandler(NoExtractableTextException.class)
    public ResponseEntity<ApiErrorResponse> handleNoExtractableText(NoExtractableTextException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(ResumeExtractionException.class)
    public ResponseEntity<ApiErrorResponse> handleResumeExtraction(ResumeExtractionException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    // ---------------------------------------------------------------------
    // Phase 6: Python AI engine integration
    // ---------------------------------------------------------------------

    /** The Python engine could not be reached (not running, refused or timed out). */
    @ExceptionHandler(AiEngineUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleAiEngineUnavailable(AiEngineUnavailableException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    /** The Python engine answered, but with an error status or an unexpected payload. */
    @ExceptionHandler(AiEngineBadResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleAiEngineBadResponse(AiEngineBadResponseException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    private static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), message));
    }
}
