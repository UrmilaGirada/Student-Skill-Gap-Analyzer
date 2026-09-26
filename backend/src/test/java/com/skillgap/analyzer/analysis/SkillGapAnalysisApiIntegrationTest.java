package com.skillgap.analyzer.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.skillgap.analyzer.job.JobDescription;
import com.skillgap.analyzer.job.JobDescriptionRepository;
import com.skillgap.analyzer.skill.ProficiencyLevel;
import com.skillgap.analyzer.skill.Skill;
import com.skillgap.analyzer.skill.SkillCategory;
import com.skillgap.analyzer.skill.SkillRepository;
import com.skillgap.analyzer.skill.StudentSkill;
import com.skillgap.analyzer.skill.StudentSkillRepository;
import com.skillgap.analyzer.student.StudentProfile;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for
 * {@code GET /api/students/{studentId}/job-descriptions/{jobDescriptionId}/skill-gap} against the real
 * MySQL database.
 *
 * <p>Phase 7B only reads persisted data, so no AI client has to be mocked: the students, skills,
 * {@code student_skills} links and job descriptions are written to MySQL and then analysed through the
 * HTTP endpoint.</p>
 *
 * <p>Every created row (job description, join rows, student-skills, the student and the catalog entries
 * this test registered) is removed afterwards; skills that already existed are never deleted.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkillGapAnalysisApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private StudentSkillRepository studentSkillRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    /** Used to prove that student skills and join rows really are (not) in the database. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private StudentProfile student;

    private final List<String> createdSkillNames = new ArrayList<>();

    @BeforeEach
    void createStudent() {
        student = studentProfileRepository.save(new StudentProfile(
                "Phase Seven B Test Student",
                "phase7b." + uniqueSuffix() + "@skillgap.local",
                "Test College",
                "Computer Science",
                2027,
                8.3));
        createdSkillNames.clear();
    }

    @AfterEach
    void removeTestData() {
        jobDescriptionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .forEach(jobDescription -> jobDescriptionRepository.delete(jobDescription));
        studentSkillRepository.deleteAll(studentSkillRepository.findByStudentId(student.getId()));
        createdSkillNames.forEach(name -> skillRepository.findByNameIgnoreCase(name)
                .filter(skill -> referenceCount(skill.getId()) == 0)
                .ifPresent(skillRepository::delete));
        studentProfileRepository.deleteById(student.getId());
    }

    // ------------------------------------------------------------------
    // 1-2. all matched / nothing matched
    // ------------------------------------------------------------------

    @Test
    void allRequiredSkillsMatchedReturnsHundredPercent() throws Exception {
        Skill java = skill("Java");
        Skill springBoot = skill("Spring Boot");
        Skill mysql = skill("MySQL");
        JobDescription jobDescription = jobDescription("Backend Developer", java, springBoot, mysql);
        attach(java, springBoot, mysql);

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.jobDescriptionId").value(jobDescription.getId()))
                .andExpect(jsonPath("$.jobTitle").value("Backend Developer"))
                .andExpect(jsonPath("$.companyName").value("Phase7B Test Company"))
                .andExpect(jsonPath("$.totalRequiredSkills").value(3))
                .andExpect(jsonPath("$.matchedSkills", hasSize(3)))
                .andExpect(jsonPath("$.partiallyMatchedSkills", hasSize(0)))
                .andExpect(jsonPath("$.missingSkills", hasSize(0)))
                .andExpect(jsonPath("$.matchedCount").value(3))
                .andExpect(jsonPath("$.partiallyMatchedCount").value(0))
                .andExpect(jsonPath("$.missingCount").value(0))
                .andExpect(jsonPath("$.matchPercentage").value(100.0));
    }

    @Test
    void noSkillsMatchedReturnsZeroPercent() throws Exception {
        JobDescription jobDescription = jobDescription("Backend Developer", skill("Java"), skill("Spring Boot"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequiredSkills").value(2))
                .andExpect(jsonPath("$.matchedSkills", hasSize(0)))
                .andExpect(jsonPath("$.missingSkills", hasSize(2)))
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.matchPercentage").value(0.0));
    }

    // ------------------------------------------------------------------
    // 3-5. partial matching and the score
    // ------------------------------------------------------------------

    @Test
    void partialMatchContributesHalfAPoint() throws Exception {
        JobDescription jobDescription = jobDescription("Java Developer", skill("Java 17"));
        attach(skill("Java"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequiredSkills").value(1))
                .andExpect(jsonPath("$.matchedSkills", hasSize(0)))
                .andExpect(jsonPath("$.partiallyMatchedSkills", hasSize(1)))
                .andExpect(jsonPath("$.partiallyMatchedSkills[0]").value("Java 17"))
                .andExpect(jsonPath("$.missingSkills", hasSize(0)))
                .andExpect(jsonPath("$.partiallyMatchedCount").value(1))
                .andExpect(jsonPath("$.matchPercentage").value(50.0));
    }

    @Test
    void mixedExactPartialAndMissingAreClassifiedAndScored() throws Exception {
        // required: 6 skills; the student has 4 exactly and "AWS Cloud" instead of the required "AWS"
        JobDescription jobDescription = jobDescription("Java Backend Developer",
                skill("Java"), skill("Spring Boot"), skill("REST APIs"),
                skill("MySQL"), skill("Git"), skill("AWS"));
        attach(skill("Java"), skill("Spring Boot"), skill("MySQL"), skill("Git"), skill("AWS Cloud"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequiredSkills").value(6))
                .andExpect(jsonPath("$.matchedSkills", hasSize(4)))
                .andExpect(jsonPath("$.partiallyMatchedSkills", hasSize(1)))
                .andExpect(jsonPath("$.partiallyMatchedSkills[0]").value("AWS"))
                .andExpect(jsonPath("$.missingSkills", hasSize(1)))
                .andExpect(jsonPath("$.missingSkills[0]").value("REST APIs"))
                .andExpect(jsonPath("$.matchedCount").value(4))
                .andExpect(jsonPath("$.partiallyMatchedCount").value(1))
                .andExpect(jsonPath("$.missingCount").value(1))
                // (4 + 0.5 * 1) / 6 * 100
                .andExpect(jsonPath("$.matchPercentage").value(75.0));
    }

    @Test
    void percentageIsRoundedToTwoDecimals() throws Exception {
        JobDescription jobDescription = jobDescription("Rounding Test",
                skill("Rounding Alpha"), skill("Rounding Beta"), skill("Rounding Gamma"));
        attach(skill("Rounding Alpha"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                // (1 + 0) / 3 * 100 = 33.333...
                .andExpect(jsonPath("$.matchPercentage").value(33.33));
    }

    // ------------------------------------------------------------------
    // 6. zero required skills
    // ------------------------------------------------------------------

    @Test
    void zeroRequiredSkillsReturnsZeroPercent() throws Exception {
        JobDescription jobDescription = jobDescription("Empty Job Description");
        attach(skill("Java"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequiredSkills").value(0))
                .andExpect(jsonPath("$.matchedSkills", hasSize(0)))
                .andExpect(jsonPath("$.partiallyMatchedSkills", hasSize(0)))
                .andExpect(jsonPath("$.missingSkills", hasSize(0)))
                .andExpect(jsonPath("$.matchPercentage").value(0.0));
    }

    // ------------------------------------------------------------------
    // 7-8. duplicates must not inflate the result
    // ------------------------------------------------------------------

    @Test
    void duplicateStudentSkillsDoNotInflateResult() throws Exception {
        // two catalog entries that normalize to the same name ("dup student java"), both attached
        Skill plain = skill("Dup Student Java");
        Skill spaced = skill("Dup  Student Java");
        JobDescription jobDescription = jobDescription("Duplicate Student Skills", plain);
        attach(plain, spaced);

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequiredSkills").value(1))
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.missingCount").value(0))
                .andExpect(jsonPath("$.matchPercentage").value(100.0));
    }

    @Test
    void duplicateRequiredSkillsAreCountedOnce() throws Exception {
        Skill plain = skill("Dup Required SQL");
        Skill spaced = skill("Dup  Required SQL");
        JobDescription jobDescription = jobDescription("Duplicate Required Skills", plain, spaced);
        attach(plain);

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequiredSkills").value(1))
                .andExpect(jsonPath("$.matchedSkills", hasSize(1)))
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.matchPercentage").value(100.0));
    }

    // ------------------------------------------------------------------
    // 9. extra student skills are ignored
    // ------------------------------------------------------------------

    @Test
    void extraStudentSkillsAreIgnored() throws Exception {
        Skill java = skill("Java");
        JobDescription jobDescription = jobDescription("Java Only", java);
        attach(java, skill("Extra Kubernetes"), skill("Extra Docker"), skill("Extra Power BI"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequiredSkills").value(1))
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.partiallyMatchedCount").value(0))
                .andExpect(jsonPath("$.missingCount").value(0))
                .andExpect(jsonPath("$.matchPercentage").value(100.0));
    }

    // ------------------------------------------------------------------
    // 10-12. ownership and 404s
    // ------------------------------------------------------------------

    @Test
    void unknownStudentReturns404() throws Exception {
        skillGap(999_999L, 1L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found"));
    }

    @Test
    void unknownJobDescriptionReturns404() throws Exception {
        skillGap(student.getId(), 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job description not found"));
    }

    @Test
    void crossStudentJobDescriptionReturns404() throws Exception {
        Skill java = skill("Java");
        JobDescription jobDescription = jobDescription("Owned By Another Student", java);
        attach(java);

        StudentProfile otherStudent = studentProfileRepository.save(new StudentProfile(
                "Phase Seven B Other Student",
                "phase7b.other." + uniqueSuffix() + "@skillgap.local",
                "Other College", "Information Technology", 2028, 7.8));
        try {
            skillGap(otherStudent.getId(), jobDescription.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Job description not found"));
        } finally {
            studentProfileRepository.deleteById(otherStudent.getId());
        }
    }

    // ------------------------------------------------------------------
    // 13. the analysis is read-only
    // ------------------------------------------------------------------

    @Test
    void analysisDoesNotModifyStudentSkills() throws Exception {
        Skill java = skill("Java");
        Skill docker = skill("Docker");
        JobDescription jobDescription = jobDescription("Read Only Check", java, skill("REST APIs"));
        attach(java, docker);

        List<String> before = studentSkillSnapshot();

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.missingCount").value(1));

        assertThat(studentSkillSnapshot()).isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_skills WHERE student_id = ?", Integer.class, student.getId()))
                .isEqualTo(before.size());
    }

    // ------------------------------------------------------------------
    // 14-16. token-boundary awareness (no substring false positives)
    // ------------------------------------------------------------------

    @Test
    void partialMatchWorksWhenTheStudentSkillIsTheBroaderName() throws Exception {
        JobDescription jobDescription = jobDescription("Spring Developer", skill("Spring"));
        attach(skill("Spring Boot"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.partiallyMatchedSkills[0]").value("Spring"))
                .andExpect(jsonPath("$.matchPercentage").value(50.0));
    }

    @Test
    void sqlDoesNotPartiallyMatchMysql() throws Exception {
        JobDescription jobDescription = jobDescription("Database Role", skill("MySQL"));
        attach(skill("SQL"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.partiallyMatchedCount").value(0))
                .andExpect(jsonPath("$.missingSkills[0]").value("MySQL"))
                .andExpect(jsonPath("$.matchPercentage").value(0.0));
    }

    @Test
    void javaDoesNotPartiallyMatchJavascript() throws Exception {
        JobDescription jobDescription = jobDescription("Frontend Role", skill("JavaScript"));
        attach(skill("Java"));

        skillGap(student.getId(), jobDescription.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.partiallyMatchedCount").value(0))
                .andExpect(jsonPath("$.missingSkills[0]").value("JavaScript"))
                .andExpect(jsonPath("$.matchPercentage").value(0.0));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Finds a catalog skill case-insensitively or creates it; only entries created here are deleted. */
    private Skill skill(String name) {
        return skillRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            createdSkillNames.add(name);
            return skillRepository.save(new Skill(name, SkillCategory.OTHER));
        });
    }

    private void attach(Skill... skills) {
        for (Skill skill : skills) {
            studentSkillRepository.save(new StudentSkill(student, skill, ProficiencyLevel.INTERMEDIATE, 1.0));
        }
    }

    private JobDescription jobDescription(String title, Skill... requiredSkills) {
        JobDescription jobDescription = new JobDescription(student, title, "Phase7B Test Company",
                "Job description created by the Phase 7B integration test");
        for (Skill skill : requiredSkills) {
            jobDescription.addRequiredSkill(skill);
        }
        return jobDescriptionRepository.save(jobDescription);
    }

    private ResultActions skillGap(Long studentId, Long jobDescriptionId) throws Exception {
        return mockMvc.perform(get("/api/students/{studentId}/job-descriptions/{jobDescriptionId}/skill-gap",
                studentId, jobDescriptionId));
    }

    /** How often a skill is still referenced by job descriptions and student skills. */
    private int referenceCount(Long skillId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT (SELECT COUNT(*) FROM job_description_skills WHERE skill_id = ?)"
                        + " + (SELECT COUNT(*) FROM student_skills WHERE skill_id = ?)",
                Integer.class, skillId, skillId);
        return (count == null) ? 0 : count;
    }

    /** Ordered snapshot of the student's persisted skills, used to prove the analysis changes nothing. */
    private List<String> studentSkillSnapshot() {
        return studentSkillRepository.findByStudentId(student.getId()).stream()
                .map(studentSkill -> studentSkill.getSkill().getId() + ":" + studentSkill.getProficiency()
                        + ":" + studentSkill.getYearsOfExperience())
                .sorted()
                .collect(Collectors.toList());
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}





