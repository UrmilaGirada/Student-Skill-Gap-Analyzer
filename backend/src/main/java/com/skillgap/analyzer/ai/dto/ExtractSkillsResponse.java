package com.skillgap.analyzer.ai.dto;

import java.util.List;

/**
 * Response of {@code POST /api/students/{studentId}/resumes/{resumeId}/extract-skills}.
 *
 * <p>Phase 6 returns the extracted skills only - nothing is persisted at this stage.</p>
 *
 * @param studentId the student who owns the resume
 * @param resumeId  the analysed resume
 * @param skills    canonical skills recognised by the Python AI engine
 */
public record ExtractSkillsResponse(Long studentId, Long resumeId, List<String> skills) {
}
