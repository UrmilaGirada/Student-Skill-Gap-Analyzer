package com.skillgap.analyzer.analysis.dto;

import java.util.List;

import com.skillgap.analyzer.analysis.RoadmapPriority;

/**
 * One ordered step of the Phase 7C learning roadmap.
 *
 * <p>Same content as {@link SkillRecommendation} plus the position: {@code order} starts at {@code 1}
 * and all HIGH (missing) steps come before the MEDIUM (partially matched) ones.</p>
 */
public record RoadmapStep(
        int order,
        String skill,
        RoadmapPriority priority,
        String reason,
        List<String> suggestedTopics) {
}
