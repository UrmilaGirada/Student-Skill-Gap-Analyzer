package com.skillgap.analyzer.skill.dto;

import com.skillgap.analyzer.skill.SkillCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/skills}.
 */
public record CreateSkillRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        String name,

        @NotNull(message = "must not be null")
        SkillCategory category) {
}
