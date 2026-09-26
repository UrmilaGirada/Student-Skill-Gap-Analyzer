package com.skillgap.analyzer.analysis.dto;

import java.util.List;

/**
 * Response of {@code GET /api/students/{studentId}/job-descriptions/{jobDescriptionId}/skill-gap}
 * (Phase 7B).
 *
 * <p>Every skill name is the canonical name stored in the {@code skills} catalog. The three lists
 * partition the job description's required skills: every required skill is either matched, partially
 * matched or missing, so {@code matchedCount + partiallyMatchedCount + missingCount == totalRequiredSkills}.</p>
 *
 * <p>The score is transparent, not opaque: see the formula documented on
 * {@link com.skillgap.analyzer.analysis.SkillGapAnalysisService}.</p>
 */
public record SkillGapAnalysisResponse(
        Long studentId,
        Long jobDescriptionId,
        String jobTitle,
        String companyName,
        int totalRequiredSkills,
        List<String> matchedSkills,
        List<String> partiallyMatchedSkills,
        List<String> missingSkills,
        int matchedCount,
        int partiallyMatchedCount,
        int missingCount,
        double matchPercentage) {
}
