package com.skillgap.analyzer.analysis.dto;

import java.util.List;

/**
 * Response of {@code GET /api/students/{studentId}/job-descriptions/{jobDescriptionId}/roadmap}
 * (Phase 7C).
 *
 * <p>The skill classification and the match percentage are exactly the Phase 7B result - the roadmap
 * only adds deterministic recommendations and an order. Everything is calculated from local data; no
 * LLM, no external API and nothing is persisted.</p>
 *
 * <p>All skill names are canonical {@code skills} catalog names.</p>
 */
public record SkillGapRoadmapResponse(
        Long studentId,
        Long jobDescriptionId,
        String jobTitle,
        String companyName,
        double matchPercentage,
        List<String> currentSkills,
        List<String> matchedSkills,
        List<String> partiallyMatchedSkills,
        List<String> missingSkills,
        List<SkillRecommendation> recommendations,
        List<RoadmapStep> roadmap) {
}
