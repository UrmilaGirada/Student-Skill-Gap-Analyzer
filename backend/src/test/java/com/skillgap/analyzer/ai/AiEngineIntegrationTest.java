package com.skillgap.analyzer.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.skillgap.analyzer.exception.AiEngineBadResponseException;
import com.skillgap.analyzer.exception.AiEngineUnavailableException;
import com.skillgap.analyzer.resume.Resume;
import com.skillgap.analyzer.resume.ResumeRepository;
import com.skillgap.analyzer.student.StudentProfile;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 6 integration tests: controller -&gt; service -&gt; (mocked) HTTP boundary -&gt; error handling,
 * against the real MySQL database.
 *
 * <p>Only {@link AiEngineClient} is mocked, so the Maven suite never requires a running Python
 * process; the live end-to-end flow is verified separately. Created students/resumes are removed
 * after each test.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiEngineIntegrationTest {

    private static final String KNOWN_TEXT = "Experienced in Java, Python, Spring Boot and AWS.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    /** Mocks only the HTTP boundary - controller, service and MySQL stay real. */
    @MockitoBean
    private AiEngineClient aiEngineClient;

    private final List<StudentProfile> createdStudents = new ArrayList<>();

    @AfterEach
    void removeTestData() {
        createdStudents.forEach(student ->
                resumeRepository.deleteAll(resumeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())));
        studentProfileRepository.deleteAll(createdStudents);
        createdStudents.clear();
    }

    @Test
    void extractsSkillsFromStoredResumeText() throws Exception {
        StudentProfile student = createStudent();
        Resume resume = createResume(student, KNOWN_TEXT);
        when(aiEngineClient.extractSkills(KNOWN_TEXT))
                .thenReturn(List.of("Java", "Python", "Spring Boot", "AWS"));

        mockMvc.perform(post("/api/students/{studentId}/resumes/{resumeId}/extract-skills",
                        student.getId(), resume.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.resumeId").value(resume.getId()))
                .andExpect(jsonPath("$.skills[0]").value("Java"))
                .andExpect(jsonPath("$.skills[1]").value("Python"))
                .andExpect(jsonPath("$.skills[2]").value("Spring Boot"))
                .andExpect(jsonPath("$.skills[3]").value("AWS"));

        // the client received exactly the stored resume text
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiEngineClient).extractSkills(textCaptor.capture());
        assertThat(textCaptor.getValue()).isEqualTo(KNOWN_TEXT);
    }

    @Test
    void returns404WhenStudentDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/students/{studentId}/resumes/{resumeId}/extract-skills", 999_999L, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found"));

        verifyNoInteractions(aiEngineClient);
    }

    @Test
    void returns404WhenResumeDoesNotExist() throws Exception {
        StudentProfile student = createStudent();

        mockMvc.perform(post("/api/students/{studentId}/resumes/{resumeId}/extract-skills",
                        student.getId(), 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resume not found"));

        verifyNoInteractions(aiEngineClient);
    }

    @Test
    void returns404WhenResumeBelongsToAnotherStudent() throws Exception {
        StudentProfile owner = createStudent();
        StudentProfile other = createStudent();
        Resume resume = createResume(owner, KNOWN_TEXT);

        mockMvc.perform(post("/api/students/{studentId}/resumes/{resumeId}/extract-skills",
                        other.getId(), resume.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resume not found"));

        verifyNoInteractions(aiEngineClient);
    }

    @Test
    void returns422WhenResumeHasNoExtractedText() throws Exception {
        StudentProfile student = createStudent();
        Resume resume = createResume(student, "   ");

        mockMvc.perform(post("/api/students/{studentId}/resumes/{resumeId}/extract-skills",
                        student.getId(), resume.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Resume has no extracted text"));

        verifyNoInteractions(aiEngineClient);
    }

    @Test
    void returns503WhenAiEngineIsUnavailable() throws Exception {
        StudentProfile student = createStudent();
        Resume resume = createResume(student, KNOWN_TEXT);
        when(aiEngineClient.extractSkills(KNOWN_TEXT))
                .thenThrow(new AiEngineUnavailableException("AI skill extraction service is unavailable"));

        mockMvc.perform(post("/api/students/{studentId}/resumes/{resumeId}/extract-skills",
                        student.getId(), resume.getId()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("AI skill extraction service is unavailable"));
    }

    @Test
    void returns502WhenAiEngineReturnsAnError() throws Exception {
        StudentProfile student = createStudent();
        Resume resume = createResume(student, KNOWN_TEXT);
        when(aiEngineClient.extractSkills(KNOWN_TEXT))
                .thenThrow(new AiEngineBadResponseException(
                        "AI skill extraction service returned an invalid response"));

        mockMvc.perform(post("/api/students/{studentId}/resumes/{resumeId}/extract-skills",
                        student.getId(), resume.getId()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("AI skill extraction service returned an invalid response"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private StudentProfile createStudent() {
        StudentProfile student = studentProfileRepository.save(new StudentProfile(
                "Phase Six Test Student",
                "phase6." + UUID.randomUUID().toString().substring(0, 8) + "@skillgap.local",
                "Test College",
                "Computer Science",
                2027,
                8.5));
        createdStudents.add(student);
        return student;
    }

    private Resume createResume(StudentProfile student, String extractedText) {
        return resumeRepository.save(
                new Resume(student, "resume.pdf", "application/pdf", 1024L, extractedText));
    }

}
