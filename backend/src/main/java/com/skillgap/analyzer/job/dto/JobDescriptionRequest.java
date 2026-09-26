package com.skillgap.analyzer.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/students/{studentId}/job-descriptions}.
 *
 * <p>The student is taken from the URL, so it is deliberately not part of the payload.</p>
 */
public record JobDescriptionRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "must be at most 200 characters")
        String title,

        @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "must be at most 200 characters")
        String companyName,

        @NotBlank(message = "must not be blank")
        @Size(max = 20000, message = "must be at most 20000 characters")
        String descriptionText) {
}
