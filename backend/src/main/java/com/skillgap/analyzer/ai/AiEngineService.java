package com.skillgap.analyzer.ai;

import java.util.List;

import com.skillgap.analyzer.ai.dto.ExtractSkillsResponse;
import com.skillgap.analyzer.exception.NoExtractableTextException;
import com.skillgap.analyzer.exception.ResourceNotFoundException;
import com.skillgap.analyzer.resume.Resume;
import com.skillgap.analyzer.resume.ResumeRepository;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.springframework.stereotype.Service;

/**
 * Phase 6 integration flow: verifies the student and resume, then asks the Python AI engine to
 * extract the canonical skills from the stored resume text.
 *
 * <p>Nothing is persisted here - analysis results will be stored in a later phase once the
 * skill-gap design is finalised. Only already-loaded fields are read, so no transaction has to be
 * held open during the HTTP call.</p>
 */
@Service
public class AiEngineService {

    private static final String STUDENT_NOT_FOUND = "Student not found";
    private static final String RESUME_NOT_FOUND = "Resume not found";
    private static final String NO_EXTRACTED_TEXT = "Resume has no extracted text";

    private final StudentProfileRepository studentProfileRepository;
    private final ResumeRepository resumeRepository;
    private final AiEngineClient aiEngineClient;

    public AiEngineService(StudentProfileRepository studentProfileRepository,
                           ResumeRepository resumeRepository,
                           AiEngineClient aiEngineClient) {
        this.studentProfileRepository = studentProfileRepository;
        this.resumeRepository = resumeRepository;
        this.aiEngineClient = aiEngineClient;
    }

    /**
     * Extracts skills from a stored resume via the Python AI engine.
     *
     * @throws ResourceNotFoundException   if the student, or the resume for that student, does not exist
     * @throws NoExtractableTextException  if the resume holds no extracted text (HTTP 422)
     * @throws com.skillgap.analyzer.exception.AiEngineUnavailableException  if the engine is down (HTTP 503)
     * @throws com.skillgap.analyzer.exception.AiEngineBadResponseException  if the engine misbehaves (HTTP 502)
     */
    public ExtractSkillsResponse extractSkillsFromResume(Long studentId, Long resumeId) {
        if (!studentProfileRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }

        // scoped to the student, so another student's resume is never reachable
        Resume resume = resumeRepository.findByIdAndStudentId(resumeId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(RESUME_NOT_FOUND));

        String text = resume.getExtractedText();
        if (text == null || text.isBlank()) {
            throw new NoExtractableTextException(NO_EXTRACTED_TEXT);
        }

        List<String> skills = aiEngineClient.extractSkills(text);
        return new ExtractSkillsResponse(studentId, resumeId, skills);
    }
}
