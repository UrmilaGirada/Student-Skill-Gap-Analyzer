package com.skillgap.analyzer.resume.dto;

import java.time.Instant;

import com.skillgap.analyzer.resume.Resume;

/**
 * Response body representing an uploaded resume.
 *
 * <p>Contains metadata and the extracted text only - no binaries, no filesystem paths.</p>
 */
public record ResumeResponse(
        Long id,
        Long studentId,
        String originalFileName,
        String contentType,
        long fileSize,
        String extractedText,
        Instant createdAt) {

    public static ResumeResponse from(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getStudent().getId(),
                resume.getOriginalFileName(),
                resume.getContentType(),
                resume.getFileSize(),
                resume.getExtractedText(),
                resume.getCreatedAt());
    }
}
