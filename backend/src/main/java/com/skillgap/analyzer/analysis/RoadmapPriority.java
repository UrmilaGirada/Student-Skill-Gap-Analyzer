package com.skillgap.analyzer.analysis;

/**
 * Priority of a learning recommendation (Phase 7C).
 *
 * <p>Deterministic mapping from the Phase 7B classification - there is no scoring and no ranking
 * beyond these two categories:</p>
 *
 * <ul>
 *   <li>{@link #HIGH} - the skill is required by the job description and completely missing</li>
 *   <li>{@link #MEDIUM} - the skill is required but only partially matched</li>
 * </ul>
 *
 * <p>Fully matched skills never produce a recommendation at all.</p>
 */
public enum RoadmapPriority {

    /** Required by the target job and missing from the student's skills. */
    HIGH,

    /** Required by the target job and only partially covered by the student's skills. */
    MEDIUM
}
