package com.skillgap.analyzer.job.dto;

import java.time.Instant;
import java.util.List;

import com.skillgap.analyzer.job.JobDescription;
import com.skillgap.analyzer.skill.Skill;

/**
 * Response body representing a stored job description.
 *
 * <p>{@code requiredSkills} carries canonical skill names from the shared {@link Skill} catalog, in
 * the order the Python AI engine detected them - no JPA entities are exposed.</p>
 */
public record JobDescriptionResponse(
        Long id,
        Long studentId,
        String title,
        String companyName,
        String descriptionText,
        List<String> requiredSkills,
        Instant createdAt,
        Instant updatedAt) {

    public static JobDescriptionResponse from(JobDescription jobDescription) {
        return new JobDescriptionResponse(
                jobDescription.getId(),
                jobDescription.getStudent().getId(),
                jobDescription.getTitle(),
                jobDescription.getCompanyName(),
                jobDescription.getDescriptionText(),
                jobDescription.getRequiredSkills().stream().map(Skill::getName).toList(),
                jobDescription.getCreatedAt(),
                jobDescription.getUpdatedAt());
    }
}
