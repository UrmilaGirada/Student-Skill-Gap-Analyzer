package com.skillgap.analyzer.job;

import java.util.LinkedHashSet;
import java.util.List;

import com.skillgap.analyzer.ai.AiEngineClient;
import com.skillgap.analyzer.exception.ResourceNotFoundException;
import com.skillgap.analyzer.job.dto.JobDescriptionRequest;
import com.skillgap.analyzer.job.dto.JobDescriptionResponse;
import com.skillgap.analyzer.skill.Skill;
import com.skillgap.analyzer.skill.SkillCategory;
import com.skillgap.analyzer.skill.SkillRepository;
import com.skillgap.analyzer.student.StudentProfile;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic of the job-description domain: it verifies the student, asks the existing Python AI
 * engine for the required skills and stores the job description together with those skills.
 *
 * <p>The AI engine is only ever reached through the Phase 6 {@link AiEngineClient} - the controller
 * never talks HTTP, and no second client exists.</p>
 *
 * <p>All reads are scoped to the owning student, so one student can never see or delete another
 * student's job description.</p>
 */
@Service
public class JobDescriptionService {

    private static final String STUDENT_NOT_FOUND = "Student not found";
    private static final String JOB_DESCRIPTION_NOT_FOUND = "Job description not found";

    private final StudentProfileRepository studentProfileRepository;
    private final SkillRepository skillRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final AiEngineClient aiEngineClient;

    public JobDescriptionService(StudentProfileRepository studentProfileRepository,
                                 SkillRepository skillRepository,
                                 JobDescriptionRepository jobDescriptionRepository,
                                 AiEngineClient aiEngineClient) {
        this.studentProfileRepository = studentProfileRepository;
        this.skillRepository = skillRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.aiEngineClient = aiEngineClient;
    }

    /**
     * Extracts the required skills from the job-description text and stores both.
     *
     * <p>Extraction runs <em>before</em> anything is persisted: when the engine is unavailable (503)
     * or answers badly (502) no job-description row is created, so a failed extraction can never leave
     * an unusable record behind. The job description, any newly registered skill and the join rows are
     * then written by a single {@code save} call in one transaction.</p>
     *
     * @throws ResourceNotFoundException if the student does not exist
     * @throws com.skillgap.analyzer.exception.AiEngineUnavailableException if the engine is unreachable (503)
     * @throws com.skillgap.analyzer.exception.AiEngineBadResponseException if the engine misbehaves (502)
     */
    public JobDescriptionResponse createJobDescription(Long studentId, JobDescriptionRequest request) {
        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        String descriptionText = request.descriptionText().trim();
        List<String> canonicalSkills = aiEngineClient.extractSkills(descriptionText);

        JobDescription jobDescription = new JobDescription(student, request.title().trim(),
                request.companyName().trim(), descriptionText);

        // The engine may legitimately report the same skill more than once (one hit per alias);
        // collapsing into a LinkedHashSet keeps the first-detection order and stores each skill once.
        for (String canonicalName : new LinkedHashSet<>(canonicalSkills)) {
            if (canonicalName != null && !canonicalName.isBlank()) {
                jobDescription.addRequiredSkill(resolveSkill(canonicalName.trim()));
            }
        }

        return JobDescriptionResponse.from(jobDescriptionRepository.save(jobDescription));
    }

    @Transactional(readOnly = true)
    public List<JobDescriptionResponse> findJobDescriptionsOfStudent(Long studentId) {
        requireStudent(studentId);
        return jobDescriptionRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(JobDescriptionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDescriptionResponse findJobDescription(Long studentId, Long jobDescriptionId) {
        requireStudent(studentId);
        return JobDescriptionResponse.from(requireJobDescription(studentId, jobDescriptionId));
    }

    /**
     * Deletes a job description and its skill associations.
     *
     * <p>The join rows are removed explicitly before the entity (Hibernate flushes collection removals
     * first), so no dangling {@code job_description_skills} rows can remain. The shared
     * {@link Skill} rows themselves are never deleted.</p>
     */
    @Transactional
    public void deleteJobDescription(Long studentId, Long jobDescriptionId) {
        requireStudent(studentId);

        JobDescription jobDescription = requireJobDescription(studentId, jobDescriptionId);
        jobDescription.clearRequiredSkills();
        jobDescriptionRepository.delete(jobDescription);
    }

    private void requireStudent(Long studentId) {
        if (!studentProfileRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }
    }

    /** Looks a job description up scoped to its owner. */
    private JobDescription requireJobDescription(Long studentId, Long jobDescriptionId) {
        return jobDescriptionRepository.findByIdAndStudentId(jobDescriptionId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_DESCRIPTION_NOT_FOUND));
    }

    /**
     * Resolves a canonical skill name returned by the engine against the existing catalog.
     *
     * <p>The lookup is case-insensitive, so an existing skill ({@code Python} vs {@code python}) is
     * reused instead of duplicated. A name the catalog does not contain yet is registered as
     * {@link SkillCategory#OTHER}: the Python catalog is the source of truth for canonical names and
     * such a skill must still be linkable. It is inserted with the job description in the same
     * transaction and becomes a normal catalog entry.</p>
     */
    private Skill resolveSkill(String canonicalName) {
        return skillRepository.findByNameIgnoreCase(canonicalName)
                .orElseGet(() -> new Skill(canonicalName, SkillCategory.OTHER));
    }

}
