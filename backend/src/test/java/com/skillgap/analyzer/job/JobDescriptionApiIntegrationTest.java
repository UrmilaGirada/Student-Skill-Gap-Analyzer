package com.skillgap.analyzer.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgap.analyzer.ai.AiEngineClient;
import com.skillgap.analyzer.exception.AiEngineBadResponseException;
import com.skillgap.analyzer.exception.AiEngineUnavailableException;
import com.skillgap.analyzer.skill.SkillRepository;
import com.skillgap.analyzer.student.StudentProfile;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration tests for {@code /api/students/{studentId}/job-descriptions} against the real MySQL
 * database.
 *
 * <p>Only the Phase 6 {@link AiEngineClient} is mocked, so the Maven suite never needs a running
 * Python process: the controller, the service, the MySQL persistence and the join table are real.
 * The live end-to-end flow is verified separately.</p>
 *
 * <p>Every created row (job descriptions, join rows, the student and any canonical skill this test
 * registered) is removed afterwards. Skill catalog entries that already existed before the test are
 * never deleted.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class JobDescriptionApiIntegrationTest {

    private static final String DESCRIPTION_TEXT =
            "We are looking for a Java developer with Spring Boot, REST APIs, MySQL, Git and AWS experience.";
    private static final List<String> CANONICAL_SKILLS =
            List.of("Java", "Spring Boot", "REST APIs", "MySQL", "Git", "AWS");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private JobDescriptionService jobDescriptionService;

    /** Reads the join table directly, so the tests can prove what is really stored. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AiEngineClient aiEngineClient;

    private StudentProfile student;

    private List<String> preExistingSkillNames = List.of();

    @BeforeEach
    void createStudent() {
        student = studentProfileRepository.save(new StudentProfile(
                "Phase Seven Test Student",
                "phase7." + uniqueSuffix() + "@skillgap.local",
                "Test College",
                "Computer Science",
                2027,
                8.2));

        // remember which catalog entries already existed, so cleanup only removes what this test created
        preExistingSkillNames = CANONICAL_SKILLS.stream()
                .filter(name -> skillRepository.findByNameIgnoreCase(name).isPresent())
                .toList();
    }

    @AfterEach
    void removeTestData() {
        jobDescriptionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).forEach(jobDescription ->
                jobDescriptionService.deleteJobDescription(student.getId(), jobDescription.getId()));
        deleteSkillsCreatedByThisTest();
        studentProfileRepository.deleteById(student.getId());
    }

    // ------------------------------------------------------------------
    // A. create
    // ------------------------------------------------------------------

    @Test
    void createJobDescriptionExtractsRequiredSkillsFromTheEngine() throws Exception {
        when(aiEngineClient.extractSkills(DESCRIPTION_TEXT)).thenReturn(CANONICAL_SKILLS);

        String body = createJobDescription("Java Backend Developer", "Example Technologies")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.title").value("Java Backend Developer"))
                .andExpect(jsonPath("$.companyName").value("Example Technologies"))
                .andExpect(jsonPath("$.descriptionText").value(DESCRIPTION_TEXT))
                .andExpect(jsonPath("$.requiredSkills", hasSize(CANONICAL_SKILLS.size())))
                .andExpect(jsonPath("$.requiredSkills[0]").value("Java"))
                .andExpect(jsonPath("$.requiredSkills[5]").value("AWS"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn().getResponse().getContentAsString();

        long jobDescriptionId = objectMapper.readTree(body).get("id").asLong();
        assertThat(jobDescriptionId).isPositive();

        // really stored in MySQL, including one join row per canonical skill
        assertThat(jobDescriptionRepository.findById(jobDescriptionId)).isPresent();
        assertThat(joinRowCount(jobDescriptionId)).isEqualTo(CANONICAL_SKILLS.size());
        CANONICAL_SKILLS.forEach(name -> assertThat(skillRepository.findByNameIgnoreCase(name)).isPresent());

        verify(aiEngineClient).extractSkills(DESCRIPTION_TEXT);
    }

    // ------------------------------------------------------------------
    // B. unknown student
    // ------------------------------------------------------------------

    @Test
    void returns404WhenStudentDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/students/{studentId}/job-descriptions", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Java Backend Developer", "Example Technologies")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found"));

        verifyNoInteractions(aiEngineClient);
    }

    @Test
    void returns404WhenListingJobDescriptionsOfUnknownStudent() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/job-descriptions", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found"));
    }

    // ------------------------------------------------------------------
    // C. validation
    // ------------------------------------------------------------------

    @Test
    void rejectsBlankOrMissingFieldsWithoutCallingTheEngine() throws Exception {
        // blank title
        mockMvc.perform(post("/api/students/{studentId}/job-descriptions", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("   ", "Example Technologies")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // blank description text
        mockMvc.perform(post("/api/students/{studentId}/job-descriptions", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Java Backend Developer", "Example Technologies", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // nothing at all
        mockMvc.perform(post("/api/students/{studentId}/job-descriptions", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(aiEngineClient);
    }

    // ------------------------------------------------------------------
    // D. list
    // ------------------------------------------------------------------

    @Test
    void listsJobDescriptionsOfStudent() throws Exception {
        when(aiEngineClient.extractSkills(DESCRIPTION_TEXT)).thenReturn(CANONICAL_SKILLS);
        createJobDescriptionAndGetId("Java Backend Developer", "Example Technologies");
        createJobDescriptionAndGetId("Cloud Engineer", "Other Company");

        mockMvc.perform(get("/api/students/{studentId}/job-descriptions", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItem("Java Backend Developer")))
                .andExpect(jsonPath("$[*].title", hasItem("Cloud Engineer")));
    }

    // ------------------------------------------------------------------
    // E. unknown job description
    // ------------------------------------------------------------------

    @Test
    void returns404ForUnknownJobDescription() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/job-descriptions/{jobDescriptionId}",
                        student.getId(), 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job description not found"));
    }

    // ------------------------------------------------------------------
    // F. cross-student isolation
    // ------------------------------------------------------------------

    @Test
    void doesNotExposeAnotherStudentsJobDescription() throws Exception {
        when(aiEngineClient.extractSkills(DESCRIPTION_TEXT)).thenReturn(CANONICAL_SKILLS);
        long jobDescriptionId = createJobDescriptionAndGetId("Java Backend Developer", "Example Technologies");

        StudentProfile otherStudent = studentProfileRepository.save(new StudentProfile(
                "Phase Seven Other Student",
                "phase7.other." + uniqueSuffix() + "@skillgap.local",
                "Other College", "Information Technology", 2028, 7.9));
        try {
            mockMvc.perform(get("/api/students/{studentId}/job-descriptions/{jobDescriptionId}",
                            otherStudent.getId(), jobDescriptionId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Job description not found"));

            mockMvc.perform(delete("/api/students/{studentId}/job-descriptions/{jobDescriptionId}",
                            otherStudent.getId(), jobDescriptionId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Job description not found"));

            // the failed attempts changed nothing
            assertThat(jobDescriptionRepository.findById(jobDescriptionId)).isPresent();
            assertThat(joinRowCount(jobDescriptionId)).isEqualTo(CANONICAL_SKILLS.size());
        } finally {
            studentProfileRepository.deleteById(otherStudent.getId());
        }
    }

    // ------------------------------------------------------------------
    // G. delete
    // ------------------------------------------------------------------

    @Test
    void deleteJobDescriptionRemovesAssociationsButKeepsSharedSkills() throws Exception {
        when(aiEngineClient.extractSkills(DESCRIPTION_TEXT)).thenReturn(CANONICAL_SKILLS);
        long jobDescriptionId = createJobDescriptionAndGetId("Java Backend Developer", "Example Technologies");
        List<Long> skillIds = CANONICAL_SKILLS.stream()
                .map(name -> skillRepository.findByNameIgnoreCase(name).orElseThrow().getId())
                .toList();

        mockMvc.perform(delete("/api/students/{studentId}/job-descriptions/{jobDescriptionId}",
                        student.getId(), jobDescriptionId))
                .andExpect(status().isNoContent());

        assertThat(jobDescriptionRepository.findById(jobDescriptionId)).isEmpty();
        assertThat(joinRowCount(jobDescriptionId)).isZero();
        // the shared catalog entries survive the delete
        skillIds.forEach(skillId -> assertThat(skillRepository.findById(skillId)).isPresent());

        mockMvc.perform(delete("/api/students/{studentId}/job-descriptions/{jobDescriptionId}",
                        student.getId(), jobDescriptionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job description not found"));
    }

    // ------------------------------------------------------------------
    // H/I. AI engine failures must not leave a job description behind
    // ------------------------------------------------------------------

    @Test
    void returns503WhenAiEngineIsUnavailableAndStoresNothing() throws Exception {
        when(aiEngineClient.extractSkills(DESCRIPTION_TEXT))
                .thenThrow(new AiEngineUnavailableException("AI skill extraction service is unavailable"));

        createJobDescription("Java Backend Developer", "Example Technologies")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("AI skill extraction service is unavailable"));

        assertThat(jobDescriptionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).isEmpty();
    }

    @Test
    void returns502WhenAiEngineReturnsAnErrorAndStoresNothing() throws Exception {
        when(aiEngineClient.extractSkills(DESCRIPTION_TEXT))
                .thenThrow(new AiEngineBadResponseException(
                        "AI skill extraction service returned an invalid response"));

        createJobDescription("Java Backend Developer", "Example Technologies")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message")
                        .value("AI skill extraction service returned an invalid response"));

        assertThat(jobDescriptionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).isEmpty();
    }

    // ------------------------------------------------------------------
    // J. duplicate canonical skills
    // ------------------------------------------------------------------

    @Test
    void storesDuplicateExtractedSkillsOnlyOnce() throws Exception {
        when(aiEngineClient.extractSkills(DESCRIPTION_TEXT))
                .thenReturn(List.of("Java", "java", "JAVA", "Java"));

        String body = createJobDescription("Java Backend Developer", "Example Technologies")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiredSkills", hasSize(1)))
                .andExpect(jsonPath("$.requiredSkills[0]").value("Java"))
                .andReturn().getResponse().getContentAsString();

        long jobDescriptionId = objectMapper.readTree(body).get("id").asLong();

        // exactly one join row and one catalog row, whatever casing the engine used
        assertThat(joinRowCount(jobDescriptionId)).isEqualTo(1);
        Integer catalogRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM skills WHERE LOWER(name) = 'java'", Integer.class);
        assertThat(catalogRows).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // K. database constraint
    // ------------------------------------------------------------------

    @Test
    void databaseEnforcesUniqueJobDescriptionSkillPair() throws Exception {
        when(aiEngineClient.extractSkills(DESCRIPTION_TEXT)).thenReturn(List.of("Java"));
        long jobDescriptionId = createJobDescriptionAndGetId("Java Backend Developer", "Example Technologies");
        Long skillId = skillRepository.findByNameIgnoreCase("Java").orElseThrow().getId();

        // bypasses the service on purpose - the composite unique constraint must refuse the duplicate
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO job_description_skills (job_description_id, skill_id) VALUES (?, ?)",
                jobDescriptionId, skillId))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(joinRowCount(jobDescriptionId)).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ResultActions createJobDescription(String title, String companyName) throws Exception {
        return mockMvc.perform(post("/api/students/{studentId}/job-descriptions", student.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(title, companyName)));
    }

    private long createJobDescriptionAndGetId(String title, String companyName) throws Exception {
        String body = createJobDescription(title, companyName)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String requestJson(String title, String companyName) throws Exception {
        return requestJson(title, companyName, DESCRIPTION_TEXT);
    }

    private String requestJson(String title, String companyName, String descriptionText) throws Exception {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("companyName", companyName);
        payload.put("descriptionText", descriptionText);
        return objectMapper.writeValueAsString(payload);
    }

    /** Join rows of one job description. */
    private int joinRowCount(Long jobDescriptionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_description_skills WHERE job_description_id = ?",
                Integer.class, jobDescriptionId);
        return (count == null) ? 0 : count;
    }

    /** Job descriptions referencing one skill. */
    private int jobDescriptionCountForSkill(Long skillId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_description_skills WHERE skill_id = ?", Integer.class, skillId);
        return (count == null) ? 0 : count;
    }

    /** Removes only the catalog entries this test registered - pre-existing skills are never touched. */
    private void deleteSkillsCreatedByThisTest() {
        CANONICAL_SKILLS.stream()
                .filter(name -> !preExistingSkillNames.contains(name))
                .forEach(name -> skillRepository.findByNameIgnoreCase(name)
                        .filter(skill -> jobDescriptionCountForSkill(skill.getId()) == 0)
                        .ifPresent(skillRepository::delete));
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}




