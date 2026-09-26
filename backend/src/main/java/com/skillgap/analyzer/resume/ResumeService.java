package com.skillgap.analyzer.resume;

import java.util.List;

import com.skillgap.analyzer.exception.ResourceNotFoundException;
import com.skillgap.analyzer.resume.dto.ResumeResponse;
import com.skillgap.analyzer.student.StudentProfile;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores resumes for a student: it verifies the student, delegates text extraction to
 * {@link ResumeTextExtractionService} and persists the metadata plus the extracted text.
 */
@Service
public class ResumeService {

    private static final String STUDENT_NOT_FOUND = "Student not found";
    private static final String RESUME_NOT_FOUND = "Resume not found";

    private final StudentProfileRepository studentProfileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeTextExtractionService resumeTextExtractionService;

    public ResumeService(StudentProfileRepository studentProfileRepository,
                         ResumeRepository resumeRepository,
                         ResumeTextExtractionService resumeTextExtractionService) {
        this.studentProfileRepository = studentProfileRepository;
        this.resumeRepository = resumeRepository;
        this.resumeTextExtractionService = resumeTextExtractionService;
    }

    /**
     * Stores an uploaded resume for a student.
     *
     * @throws ResourceNotFoundException if the student does not exist
     */
    public ResumeResponse storeResume(Long studentId, MultipartFile file) {
        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        String extractedText = resumeTextExtractionService.extractText(file);

        Resume resume = new Resume(
                student,
                sanitizeFileName(file.getOriginalFilename()),
                file.getContentType(),
                file.getSize(),
                extractedText);

        return ResumeResponse.from(resumeRepository.save(resume));
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> findResumesOfStudent(Long studentId) {
        requireStudent(studentId);
        return resumeRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(ResumeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse findResume(Long studentId, Long resumeId) {
        requireStudent(studentId);
        return resumeRepository.findByIdAndStudentId(resumeId, studentId)
                .map(ResumeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(RESUME_NOT_FOUND));
    }

    /** Deletes a resume record. Nothing is removed from disk because no binaries are stored. */
    public void deleteResume(Long studentId, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndStudentId(resumeId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(RESUME_NOT_FOUND));

        resumeRepository.delete(resume);
    }

    private void requireStudent(Long studentId) {
        if (!studentProfileRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }
    }

    /**
     * Keeps only the file name: directory components (which may come from the client) and control
     * characters are stripped, so no server or client path is ever stored or returned.
     */
    static String sanitizeFileName(String originalFileName) {
        if (originalFileName == null) {
            return "resume";
        }
        String fileName = originalFileName.replace('\\', '/');
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }
        fileName = fileName.replaceAll("\\p{Cntrl}", "").trim();

        if (fileName.isEmpty()) {
            return "resume";
        }
        return (fileName.length() > 255) ? fileName.substring(0, 255) : fileName;
    }
}
