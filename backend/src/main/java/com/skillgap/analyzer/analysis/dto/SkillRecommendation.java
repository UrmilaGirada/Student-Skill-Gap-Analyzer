package com.skillgap.analyzer.analysis.dto;

import java.util.List;

import com.skillgap.analyzer.analysis.RoadmapPriority;

/**
 * One learning recommendation of the Phase 7C roadmap.
 *
 * <p>{@code suggestedTopics} comes from the local, deterministic
 * {@link com.skillgap.analyzer.analysis.LearningTopicCatalog} - not from an external service, an LLM
 * or a course provider.</p>
 */
public record SkillRecommendation(
        String skill,
        RoadmapPriority priority,
        String reason,
        List<String> suggestedTopics) {
}
