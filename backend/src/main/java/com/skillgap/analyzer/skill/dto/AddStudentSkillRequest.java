package com.skillgap.analyzer.skill.dto;

import com.skillgap.analyzer.skill.ProficiencyLevel;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/students/{studentId}/skills}.
 */
public record AddStudentSkillRequest(

        @NotNull(message = "must not be null")
        Long skillId,

        @NotNull(message = "must not be null")
        ProficiencyLevel proficiency,

        @NotNull(message = "must not be null")
        @DecimalMin(value = "0.0", message = "must be greater than or equal to 0")
        @DecimalMax(value = "60.0", message = "must be at most 60")
        Double yearsOfExperience) {
}
