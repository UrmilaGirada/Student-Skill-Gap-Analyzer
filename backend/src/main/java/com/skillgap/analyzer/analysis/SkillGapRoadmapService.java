package com.skillgap.analyzer.analysis;

import java.util.ArrayList;
import java.util.List;

import com.skillgap.analyzer.analysis.dto.RoadmapStep;
import com.skillgap.analyzer.analysis.dto.SkillGapAnalysisResponse;
import com.skillgap.analyzer.analysis.dto.SkillGapRoadmapResponse;
import com.skillgap.analyzer.analysis.dto.SkillRecommendation;
import com.skillgap.analyzer.skill.StudentSkillRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 7C: turns the Phase 7B skill-gap result into a deterministic learning roadmap.
 *
 * <p><b>Read-only:</b> nothing is persisted, no {@code MatchResult} entity or table exists, and the
 * student's skills are never modified. The endpoint only reads what Phase 7A/7B already stored.</p>
 *
 * <p>Matching, classification and the match percentage are taken verbatim from
 * {@link SkillGapAnalysisService} - this service reuses that result instead of duplicating the
 * normalization, exact-match, partial-match or scoring logic. It only adds priority, a reason, local
 * learning topics and the order.</p>
 *
 * <p><b>Priority rule:</b> a missing required skill is {@link RoadmapPriority#HIGH}, a partially
 * matched required skill is {@link RoadmapPriority#MEDIUM}, and fully matched skills are never
 * recommended.</p>
 *
 * <p><b>Order:</b> every HIGH step comes before every MEDIUM step; inside one priority the Phase 7B
 * order (the job description's required-skill order) is preserved. The content comes from the local
 * {@link LearningTopicCatalog} - no LLM, no external API.</p>
 */
@Service
public class SkillGapRoadmapService {

    private static final String HIGH_REASON =
            "Required by the target job but missing from the student's skills.";
    private static final String MEDIUM_REASON =
            "Required by the target job, but only partially covered by the student's current skills.";

    private final SkillGapAnalysisService skillGapAnalysisService;
    private final StudentSkillRepository studentSkillRepository;

    public SkillGapRoadmapService(SkillGapAnalysisService skillGapAnalysisService,
                                  StudentSkillRepository studentSkillRepository) {
        this.skillGapAnalysisService = skillGapAnalysisService;
        this.studentSkillRepository = studentSkillRepository;
    }

    /**
     * Builds the learning roadmap for one of the student's job descriptions.
     *
     * @throws com.skillgap.analyzer.exception.ResourceNotFoundException if the student does not exist,
     *         or the job description does not exist or belongs to another student (HTTP 404)
     */
    @Transactional(readOnly = true)
    public SkillGapRoadmapResponse buildRoadmap(Long studentId, Long jobDescriptionId) {
        // Phase 7B remains the single source of truth for the classification and the score
        SkillGapAnalysisResponse analysis = skillGapAnalysisService.analyseSkillGap(studentId, jobDescriptionId);

        // recommendations: missing (HIGH) first, then partially matched (MEDIUM); matched skills are skipped
        List<SkillRecommendation> recommendations = new ArrayList<>();
        analysis.missingSkills().forEach(skill ->
                recommendations.add(recommend(skill, RoadmapPriority.HIGH, HIGH_REASON)));
        analysis.partiallyMatchedSkills().forEach(skill ->
                recommendations.add(recommend(skill, RoadmapPriority.MEDIUM, MEDIUM_REASON)));

        List<RoadmapStep> roadmap = new ArrayList<>();
        for (int index = 0; index < recommendations.size(); index++) {
            SkillRecommendation recommendation = recommendations.get(index);
            roadmap.add(new RoadmapStep(
                    index + 1,
                    recommendation.skill(),
                    recommendation.priority(),
                    recommendation.reason(),
                    recommendation.suggestedTopics()));
        }

        return new SkillGapRoadmapResponse(
                analysis.studentId(),
                analysis.jobDescriptionId(),
                analysis.jobTitle(),
                analysis.companyName(),
                analysis.matchPercentage(),
                currentSkills(studentId),
                analysis.matchedSkills(),
                analysis.partiallyMatchedSkills(),
                analysis.missingSkills(),
                recommendations,
                roadmap);
    }

    private static SkillRecommendation recommend(String skill, RoadmapPriority priority, String reason) {
        return new SkillRecommendation(skill, priority, reason, LearningTopicCatalog.topicsFor(skill));
    }

    /**
     * The student's persisted skills (canonical names), sorted case-insensitively so the response is
     * stable. Phase 7B does not expose them, so they are read here - nothing else is duplicated.
     */
    private List<String> currentSkills(Long studentId) {
        return studentSkillRepository.findByStudentId(studentId).stream()
                .map(studentSkill -> studentSkill.getSkill().getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
