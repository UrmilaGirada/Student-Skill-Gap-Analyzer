package com.skillgap.analyzer.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.skillgap.analyzer.analysis.dto.SkillGapAnalysisResponse;
import com.skillgap.analyzer.exception.ResourceNotFoundException;
import com.skillgap.analyzer.job.JobDescription;
import com.skillgap.analyzer.job.JobDescriptionRepository;
import com.skillgap.analyzer.skill.Skill;
import com.skillgap.analyzer.skill.StudentSkill;
import com.skillgap.analyzer.skill.StudentSkillRepository;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 7B: compares the skills a student has ({@code student_skills}) with the skills one of the
 * student's job descriptions requires ({@code job_descriptions -> job_description_skills}).
 *
 * <p><b>Read-only:</b> nothing is persisted, the student's stored skills never change, and job skills
 * are never copied into {@code student_skills}. The analysis is recalculated per request.</p>
 *
 * <p><b>Matching rule</b> (deterministic - no NLP, no fuzzy or semantic matching): names are
 * normalized (lower case, trimmed, whitespace collapsed); a required skill is {@code MATCHED} when the
 * normalized names are exactly equal; {@code PARTIALLY_MATCHED} when one normalized name occurs in the
 * other as a whole token or a contiguous run of whole tokens ({@code Java} in {@code Java 17},
 * {@code Spring} in {@code Spring Boot}); {@code MISSING} otherwise. Token-boundary awareness keeps
 * {@code SQL} from partially matching {@code MySQL} and {@code Java} from matching {@code JavaScript}.</p>
 *
 * <p><b>Score:</b> {@code (matchedCount + 0.5 * partiallyMatchedCount) / totalRequiredSkills * 100},
 * rounded half-up to two decimals, {@code 0.0} when no skills are required. Classification is per
 * required skill, so duplicates cannot inflate the result; extra student skills are ignored.</p>
 */
@Service
public class SkillGapAnalysisService {

    private static final String STUDENT_NOT_FOUND = "Student not found";
    private static final String JOB_DESCRIPTION_NOT_FOUND = "Job description not found";

    private final StudentProfileRepository studentProfileRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final JobDescriptionRepository jobDescriptionRepository;

    public SkillGapAnalysisService(StudentProfileRepository studentProfileRepository,
                                   StudentSkillRepository studentSkillRepository,
                                   JobDescriptionRepository jobDescriptionRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    /**
     * Analyses the skill gap between a student and one of that student's job descriptions.
     *
     * @throws ResourceNotFoundException if the student does not exist, or the job description does not
     *                                   exist or belongs to another student (HTTP 404)
     */
    @Transactional(readOnly = true)
    public SkillGapAnalysisResponse analyseSkillGap(Long studentId, Long jobDescriptionId) {
        if (!studentProfileRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }

        // scoped to the owner, so another student's job description can never be analysed
        JobDescription jobDescription = jobDescriptionRepository.findByIdAndStudentId(jobDescriptionId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_DESCRIPTION_NOT_FOUND));

        // normalized name -> canonical name; the first spelling wins, so duplicates are counted once
        Map<String, String> requiredSkills = new LinkedHashMap<>();
        for (Skill skill : jobDescription.getRequiredSkills()) {
            requiredSkills.putIfAbsent(normalize(skill.getName()), skill.getName());
        }

        Map<String, String> studentSkills = new LinkedHashMap<>();
        for (StudentSkill studentSkill : studentSkillRepository.findByStudentId(studentId)) {
            String name = studentSkill.getSkill().getName();
            studentSkills.putIfAbsent(normalize(name), name);
        }

        List<String> matchedSkills = new ArrayList<>();
        List<String> partiallyMatchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (Map.Entry<String, String> required : requiredSkills.entrySet()) {
            if (studentSkills.containsKey(required.getKey())) {
                matchedSkills.add(required.getValue());
            } else if (hasTokenAlignedContainment(required.getKey(), studentSkills.keySet())) {
                partiallyMatchedSkills.add(required.getValue());
            } else {
                missingSkills.add(required.getValue());
            }
        }

        int totalRequiredSkills = requiredSkills.size();
        return new SkillGapAnalysisResponse(
                studentId,
                jobDescriptionId,
                jobDescription.getTitle(),
                jobDescription.getCompanyName(),
                totalRequiredSkills,
                matchedSkills,
                partiallyMatchedSkills,
                missingSkills,
                matchedSkills.size(),
                partiallyMatchedSkills.size(),
                missingSkills.size(),
                matchPercentage(totalRequiredSkills, matchedSkills.size(), partiallyMatchedSkills.size()));
    }

    /**
     * Transparent score: {@code (matched + 0.5 * partiallyMatched) / totalRequired * 100}, rounded
     * half-up to two decimals. A job description without required skills scores {@code 0.0}.
     */
    static double matchPercentage(int totalRequiredSkills, int matchedCount, int partiallyMatchedCount) {
        if (totalRequiredSkills <= 0) {
            return 0.0;
        }
        double raw = (matchedCount + 0.5 * partiallyMatchedCount) / totalRequiredSkills * 100.0;
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Lower case, trimmed, collapsed whitespace: {@code "  Spring   Boot "} becomes {@code "spring boot"}. */
    static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** True when the required skill is a token-aligned part of a student skill, or vice versa. */
    private static boolean hasTokenAlignedContainment(String requiredNormalized, Set<String> studentNormalized) {
        return studentNormalized.stream().anyMatch(student ->
                containsTokenRun(requiredNormalized, student) || containsTokenRun(student, requiredNormalized));
    }

    /**
     * True when {@code needle} occurs in {@code haystack} as a whole token or as a contiguous run of
     * whole tokens. Tokens are split on every non-alphanumeric character, so {@code sql} is not found
     * in {@code mysql} and {@code java} is not found in {@code javascript}, while {@code java} <em>is</em>
     * found in {@code java 17} and {@code spring} in {@code spring boot}.
     */
    static boolean containsTokenRun(String haystack, String needle) {
        List<String> haystackTokens = tokenize(haystack);
        List<String> needleTokens = tokenize(needle);

        if (needleTokens.isEmpty() || needleTokens.size() > haystackTokens.size()) {
            return false;
        }
        for (int start = 0; start + needleTokens.size() <= haystackTokens.size(); start++) {
            if (haystackTokens.subList(start, start + needleTokens.size()).equals(needleTokens)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> tokenize(String value) {
        return Arrays.stream(value.split("[^\\p{Alnum}]+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

}
