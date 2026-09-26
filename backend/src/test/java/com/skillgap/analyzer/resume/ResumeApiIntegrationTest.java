package com.skillgap.analyzer.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgap.analyzer.student.StudentProfile;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration tests for {@code /api/students/{studentId}/resumes} against real MySQL with the real
 * Apache Tika extraction - nothing on the upload path is mocked.
 *
 * <p>Documents are generated in memory by {@link ResumeTestDocuments}: no binaries and no personal
 * data are committed. Created students/resumes are deleted after each test.
 *
 * <p>An upload larger than 10 MB is covered at service level in {@code ResumeTextExtractionServiceTest}
 * (MockMvc bypasses the servlet multipart limits that produce the runtime 413 response).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResumeApiIntegrationTest {

    private static final String PDF = "application/pdf";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    private final List<StudentProfile> createdStudents = new ArrayList<>();

    @AfterEach
    void removeTestData() {
        createdStudents.forEach(student ->
                resumeRepository.deleteAll(resumeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())));
        studentProfileRepository.deleteAll(createdStudents);
        createdStudents.clear();
    }

    // ------------------------------------------------------------------
    // Upload
    // ------------------------------------------------------------------

    @Test
    void uploadsPdfResumeExtractsTextAndPersistsIt() throws Exception {
        StudentProfile student = createStudent();

        String body = upload(student, pdfFile("resume.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.originalFileName").value("resume.pdf"))
                .andExpect(jsonPath("$.contentType").value(PDF))
                .andExpect(jsonPath("$.fileSize").value(greaterThan(0)))
                .andExpect(jsonPath("$.extractedText").value(containsString("TEST CANDIDATE")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();

        long resumeId = objectMapper.readTree(body).get("id").asLong();

        // really stored in MySQL, with non-blank text and the right owner
        Resume stored = resumeRepository.findById(resumeId).orElseThrow();
        assertThat(stored.getStudent().getId()).isEqualTo(student.getId());
        assertThat(stored.getExtractedText()).isNotBlank();
        assertThat(stored.getFileSize()).isPositive();

        // GET single
        mockMvc.perform(get("/api/students/{studentId}/resumes/{resumeId}", student.getId(), resumeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resumeId))
                .andExpect(jsonPath("$.extractedText").value(containsString("TEST CANDIDATE")));
    }

    @Test
    void uploadsDocxResumeAndExtractsText() throws Exception {
        StudentProfile student = createStudent();
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx", DOCX,
                ResumeTestDocuments.docxWithText(ResumeTestDocuments.SAMPLE_TEXT));

        upload(student, file)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value(DOCX))
                .andExpect(jsonPath("$.extractedText").value(containsString("TEST CANDIDATE")));

        assertThat(resumeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).hasSize(1);
    }

    @Test
    void sanitizesClientSuppliedFileName() throws Exception {
        StudentProfile student = createStudent();

        upload(student, pdfFile("..\\..\\evil\\resume.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("resume.pdf"));
    }

    // ------------------------------------------------------------------
    // Error cases
    // ------------------------------------------------------------------

    @Test
    void returns404WhenStudentDoesNotExist() throws Exception {
        mockMvc.perform(multipart("/api/students/{studentId}/resumes", 999_999L).file(pdfFile("resume.pdf")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found"));

        mockMvc.perform(get("/api/students/{studentId}/resumes", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found"));
    }

    @Test
    void returns400WhenFilePartIsMissing() throws Exception {
        StudentProfile student = createStudent();

        mockMvc.perform(multipart("/api/students/{studentId}/resumes", student.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Resume file is required"));
    }

    @Test
    void returns415ForUnsupportedContentType() throws Exception {
        StudentProfile student = createStudent();
        MockMultipartFile txt = new MockMultipartFile("file", "notes.txt", "text/plain",
                "plain text".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        upload(student, txt)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.message").value("Unsupported resume file type"));

        assertThat(resumeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).isEmpty();
    }

    @Test
    void returns422WhenDocumentHasNoExtractableText() throws Exception {
        StudentProfile student = createStudent();
        MockMultipartFile scanned = new MockMultipartFile("file", "scanned.pdf", PDF, ResumeTestDocuments.blankPdf());

        upload(student, scanned)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("No extractable text found in resume"));

        assertThat(resumeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).isEmpty();
    }

    // ------------------------------------------------------------------
    // List, isolation, delete
    // ------------------------------------------------------------------

    @Test
    void listsOnlyResumesOfTheRequestedStudent() throws Exception {
        StudentProfile alice = createStudent();
        StudentProfile bob = createStudent();
        long aliceResume = uploadedResumeId(alice, pdfFile("alice.pdf"));
        long bobResume = uploadedResumeId(bob, pdfFile("bob.pdf"));

        String body = mockMvc.perform(get("/api/students/{studentId}/resumes", alice.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Long> listedIds = new ArrayList<>();
        objectMapper.readTree(body).forEach(node -> listedIds.add(node.get("id").asLong()));
        assertThat(listedIds).contains(aliceResume).doesNotContain(bobResume);
    }

    @Test
    void doesNotExposeResumeToAnotherStudent() throws Exception {
        StudentProfile owner = createStudent();
        StudentProfile other = createStudent();
        long resumeId = uploadedResumeId(owner, pdfFile("owner.pdf"));

        mockMvc.perform(get("/api/students/{studentId}/resumes/{resumeId}", other.getId(), resumeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resume not found"));

        mockMvc.perform(delete("/api/students/{studentId}/resumes/{resumeId}", other.getId(), resumeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resume not found"));

        // the failed attempts must not have removed it
        assertThat(resumeRepository.findById(resumeId)).isPresent();
    }

    @Test
    void deletesResumeAndThenReturns404() throws Exception {
        StudentProfile student = createStudent();
        long resumeId = uploadedResumeId(student, pdfFile("resume.pdf"));

        mockMvc.perform(delete("/api/students/{studentId}/resumes/{resumeId}", student.getId(), resumeId))
                .andExpect(status().isNoContent());

        assertThat(resumeRepository.findById(resumeId)).isEmpty();

        mockMvc.perform(get("/api/students/{studentId}/resumes/{resumeId}", student.getId(), resumeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resume not found"));

        mockMvc.perform(delete("/api/students/{studentId}/resumes/{resumeId}", student.getId(), resumeId))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private StudentProfile createStudent() {
        StudentProfile student = studentProfileRepository.save(new StudentProfile(
                "Phase Four Test Student",
                "phase4." + UUID.randomUUID().toString().substring(0, 8) + "@skillgap.local",
                "Test College",
                "Computer Science",
                2027,
                8.5));
        createdStudents.add(student);
        return student;
    }

    private static MockMultipartFile pdfFile(String fileName) throws IOException {
        return new MockMultipartFile("file", fileName, PDF,
                ResumeTestDocuments.pdfWithText(ResumeTestDocuments.SAMPLE_TEXT));
    }

    private ResultActions upload(StudentProfile student, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/students/{studentId}/resumes", student.getId()).file(file));
    }

    private long uploadedResumeId(StudentProfile student, MockMultipartFile file) throws Exception {
        MvcResult result = upload(student, file).andExpect(status().isCreated()).andReturn();
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        return created.get("id").asLong();
    }
}
