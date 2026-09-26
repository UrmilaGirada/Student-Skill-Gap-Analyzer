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
 * {@code GET /api/students/{studentId}/job-descriptions/{jobDescriptionId}/roadmap} against the real
 * MySQL database.
 *
 * <p>The roadmap is read-only and needs no AI engine, so the students, skills, {@code student_skills}
 * links and job descriptions are written to MySQL and then queried through the HTTP endpoint.</p>
 *
 * <p>Every created row is removed afterwards; skills that already existed are never deleted.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkillGapRoadmapApiIntegrationTest {

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

    /** Used for the "does not persist anything" checks and for safe cleanup. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private StudentProfile student;

    private final List<String> createdSkillNames = new ArrayList<>();

    @BeforeEach
    void createStudent() {
        student = studentProfileRepository.save(new StudentProfile(
                "Phase Seven C Test Student",
                "phase7c." + uniqueSuffix() + "@skillgap.local",
                "Test College",
                "Computer Science",
                2027,
                8.4));
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

    @Test
    void roadmapReturnsMissingSkillsAsHighPriority() throws Exception {
        Skill java = skill("Java");
        Skill restApis = skill("REST APIs");
        attach(student, java);
        JobDescription jd = jobDescription(student, "Backend Dev", List.of(java, restApis));

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.jobDescriptionId").value(jd.getId()))
                .andExpect(jsonPath("$.matchedSkills", hasSize(1)))
                .andExpect(jsonPath("$.missingSkills", hasSize(1)))
                .andExpect(jsonPath("$.missingSkills[0]").value("REST APIs"))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].skill").value("REST APIs"))
                .andExpect(jsonPath("$.recommendations[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.recommendations[0].reason")
                        .value("Required by the target job but missing from the student's skills."))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[0]").value("HTTP methods"))
                .andExpect(jsonPath("$.roadmap", hasSize(1)))
                .andExpect(jsonPath("$.roadmap[0].order").value(1))
                .andExpect(jsonPath("$.roadmap[0].skill").value("REST APIs"))
                .andExpect(jsonPath("$.roadmap[0].priority").value("HIGH"));
    }

    @Test
    void roadmapReturnsPartialSkillsAsMediumPriority() throws Exception {
        Skill java = skill("Java");
        Skill java17 = skill("Java 17");
        attach(student, java);
        JobDescription jd = jobDescription(student, "Java 17 Engineer", List.of(java17));

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedSkills").isEmpty())
                .andExpect(jsonPath("$.partiallyMatchedSkills", hasSize(1)))
                .andExpect(jsonPath("$.partiallyMatchedSkills[0]").value("Java 17"))
                .andExpect(jsonPath("$.missingSkills").isEmpty())
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].skill").value("Java 17"))
                .andExpect(jsonPath("$.recommendations[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.recommendations[0].reason")
                        .value("Required by the target job, but only partially covered by the student's current skills."))
                .andExpect(jsonPath("$.roadmap", hasSize(1)))
                .andExpect(jsonPath("$.roadmap[0].order").value(1))
                .andExpect(jsonPath("$.roadmap[0].skill").value("Java 17"))
                .andExpect(jsonPath("$.roadmap[0].priority").value("MEDIUM"));
    }

    @Test
    void matchedSkillsAreNotRecommended() throws Exception {
        Skill java = skill("Java");
        attach(student, java);
        JobDescription jd = jobDescription(student, "Java Dev", List.of(java));

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage").value(100.0))
                .andExpect(jsonPath("$.matchedSkills", hasSize(1)))
                .andExpect(jsonPath("$.matchedSkills[0]").value("Java"))
                .andExpect(jsonPath("$.partiallyMatchedSkills").isEmpty())
                .andExpect(jsonPath("$.missingSkills").isEmpty())
                .andExpect(jsonPath("$.recommendations").isEmpty())
                .andExpect(jsonPath("$.roadmap").isEmpty());
    }

    @Test
    void missingSkillsComeBeforePartialSkills() throws Exception {
        Skill java = skill("Java");
        Skill java17 = skill("Java 17");
        Skill restApis = skill("REST APIs");
        attach(student, java);
        JobDescription jd = jobDescription(student, "Backend Dev", List.of(java17, restApis));

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations", hasSize(2)))
                .andExpect(jsonPath("$.recommendations[0].skill").value("REST APIs"))
                .andExpect(jsonPath("$.recommendations[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.recommendations[1].skill").value("Java 17"))
                .andExpect(jsonPath("$.recommendations[1].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.roadmap", hasSize(2)))
                .andExpect(jsonPath("$.roadmap[0].order").value(1))
                .andExpect(jsonPath("$.roadmap[0].skill").value("REST APIs"))
                .andExpect(jsonPath("$.roadmap[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.roadmap[1].order").value(2))
                .andExpect(jsonPath("$.roadmap[1].skill").value("Java 17"))
                .andExpect(jsonPath("$.roadmap[1].priority").value("MEDIUM"));
    }

    @Test
    void roadmapContainsDeterministicSuggestedTopics() throws Exception {
        Skill mysql = skill("MySQL");
        JobDescription jd = jobDescription(student, "Database Admin", List.of(mysql));

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].skill").value("MySQL"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics", hasSize(5)))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[0]").value("SELECT / WHERE"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[1]").value("JOINs"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[2]").value("GROUP BY / HAVING"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[3]").value("Subqueries"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[4]").value("Indexes"))
                .andExpect(jsonPath("$.roadmap[0].suggestedTopics", hasSize(5)))
                .andExpect(jsonPath("$.roadmap[0].suggestedTopics[0]").value("SELECT / WHERE"));
    }

    @Test
    void unmappedSkillUsesGenericTopics() throws Exception {
        Skill obscure = skill("ObscureSkill" + uniqueSuffix());
        JobDescription jd = jobDescription(student, "Niche Dev", List.of(obscure));

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].skill").value(obscure.getName()))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics", hasSize(5)))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[0]").value("fundamentals"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[1]").value("core concepts"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[2]").value("practical implementation"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[3]").value("common interview questions"))
                .andExpect(jsonPath("$.recommendations[0].suggestedTopics[4]").value("mini project"));
    }

    @Test
    void unknownStudentReturns404() throws Exception {
        Skill java = skill("Java");
        JobDescription jd = jobDescription(student, "Java Dev", List.of(java));

        roadmap(999999L, jd.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found"));
    }

    @Test
    void unknownJobDescriptionReturns404() throws Exception {
        roadmap(student.getId(), 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job description not found"));
    }

    @Test
    void crossStudentJobDescriptionReturns404() throws Exception {
        StudentProfile otherStudent = studentProfileRepository.save(new StudentProfile(
                "Other Student",
                "other." + uniqueSuffix() + "@skillgap.local",
                "Other College",
                "IT",
                2026,
                7.5));
        try {
            Skill java = skill("Java");
            JobDescription otherJd = jobDescription(otherStudent, "Other Dev", List.of(java));

            roadmap(student.getId(), otherJd.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Job description not found"));
        } finally {
            jobDescriptionRepository.findByStudentIdOrderByCreatedAtDesc(otherStudent.getId())
                    .forEach(jobDescriptionRepository::delete);
            studentProfileRepository.deleteById(otherStudent.getId());
        }
    }

    @Test
    void roadmapDoesNotModifyStudentSkills() throws Exception {
        Skill java = skill("Java");
        Skill restApis = skill("REST APIs");
        attach(student, java);
        JobDescription jd = jobDescription(student, "Backend Dev", List.of(java, restApis));

        List<Long> beforeIds = studentSkillSnapshot();

        roadmap(student.getId(), jd.getId()).andExpect(status().isOk());

        List<Long> afterIds = studentSkillSnapshot();
        assertThat(afterIds).containsExactlyElementsOf(beforeIds);
    }

    @Test
    void roadmapDoesNotPersistAnything() throws Exception {
        Skill java = skill("Java");
        Skill restApis = skill("REST APIs");
        attach(student, java);
        JobDescription jd = jobDescription(student, "Backend Dev", List.of(java, restApis));

        long studentSkillsBefore = countRows("student_skills");
        long jobDescriptionsBefore = countRows("job_descriptions");
        long jdSkillsBefore = countRows("job_description_skills");
        long skillsBefore = countRows("skills");

        roadmap(student.getId(), jd.getId()).andExpect(status().isOk());

        assertThat(countRows("student_skills")).isEqualTo(studentSkillsBefore);
        assertThat(countRows("job_descriptions")).isEqualTo(jobDescriptionsBefore);
        assertThat(countRows("job_description_skills")).isEqualTo(jdSkillsBefore);
        assertThat(countRows("skills")).isEqualTo(skillsBefore);
    }

    @Test
    void noRequiredSkillsReturnsEmptyRoadmap() throws Exception {
        JobDescription jd = jobDescription(student, "Generalist", List.of());

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage").value(0.0))
                .andExpect(jsonPath("$.matchedSkills").isEmpty())
                .andExpect(jsonPath("$.partiallyMatchedSkills").isEmpty())
                .andExpect(jsonPath("$.missingSkills").isEmpty())
                .andExpect(jsonPath("$.recommendations").isEmpty())
                .andExpect(jsonPath("$.roadmap").isEmpty());
    }

    @Test
    void duplicateRequiredSkillsDoNotProduceDuplicateRoadmapEntries() throws Exception {
        Skill sqlA = skill("Roadmap SQL " + uniqueSuffix());
        Skill sqlB = skill("  " + sqlA.getName().toLowerCase() + "  ");
        JobDescription jd = jobDescription(student, "SQL Dev", List.of(sqlA, sqlB));

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missingSkills", hasSize(1)))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].skill").value(sqlA.getName()))
                .andExpect(jsonPath("$.roadmap", hasSize(1)))
                .andExpect(jsonPath("$.roadmap[0].order").value(1));
    }

    @Test
    void roadmapCombinesAllThreeCategories() throws Exception {
        // Matched: Java. Partial: AWS (student has AWS Cloud). Missing: REST APIs.
        // matchPercentage = (1 + 0.5) / 3 * 100 = 50.0%
        // Recommendations/roadmap: 2 items, REST APIs (HIGH) then AWS (MEDIUM).
        Skill java = skill("Java");
        Skill awsCloud = skill("AWS Cloud");
        attach(student, java);
        attach(student, awsCloud);

        Skill aws = skill("AWS");
        Skill restApis = skill("REST APIs");
        JobDescription jd = jobDescription(student, "Fullstack", List.of(java, aws, restApis));

        roadmap(student.getId(), jd.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage").value(50.0))
                .andExpect(jsonPath("$.matchedSkills", hasSize(1)))
                .andExpect(jsonPath("$.matchedSkills[0]").value("Java"))
                .andExpect(jsonPath("$.partiallyMatchedSkills", hasSize(1)))
                .andExpect(jsonPath("$.partiallyMatchedSkills[0]").value("AWS"))
                .andExpect(jsonPath("$.missingSkills", hasSize(1)))
                .andExpect(jsonPath("$.missingSkills[0]").value("REST APIs"))
                .andExpect(jsonPath("$.recommendations", hasSize(2)))
                .andExpect(jsonPath("$.recommendations[0].skill").value("REST APIs"))
                .andExpect(jsonPath("$.recommendations[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.recommendations[1].skill").value("AWS"))
                .andExpect(jsonPath("$.recommendations[1].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.roadmap", hasSize(2)))
                .andExpect(jsonPath("$.roadmap[0].order").value(1))
                .andExpect(jsonPath("$.roadmap[0].skill").value("REST APIs"))
                .andExpect(jsonPath("$.roadmap[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.roadmap[1].order").value(2))
                .andExpect(jsonPath("$.roadmap[1].skill").value("AWS"))
                .andExpect(jsonPath("$.roadmap[1].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.currentSkills", hasSize(2)));
    }

    // --- Helpers ---

    private ResultActions roadmap(Long studentId, Long jobDescriptionId) throws Exception {
        return mockMvc.perform(get("/api/students/{studentId}/job-descriptions/{jobDescriptionId}/roadmap",
                studentId, jobDescriptionId));
    }

    private Skill skill(String name) {
        return skillRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            createdSkillNames.add(name);
            return skillRepository.save(new Skill(name, SkillCategory.OTHER));
        });
    }

    private StudentSkill attach(StudentProfile targetStudent, Skill skill) {
        return studentSkillRepository.save(new StudentSkill(targetStudent, skill, ProficiencyLevel.INTERMEDIATE, 1.0));
    }

    private JobDescription jobDescription(StudentProfile targetStudent, String title, List<Skill> skills) {
        JobDescription jd = new JobDescription(
                targetStudent,
                title,
                "Acme Corp",
                "Job description created by the Phase 7C integration test");
        skills.forEach(jd::addRequiredSkill);
        return jobDescriptionRepository.save(jd);
    }

    private List<Long> studentSkillSnapshot() {
        return studentSkillRepository.findByStudentId(student.getId()).stream()
                .map(StudentSkill::getId)
                .collect(Collectors.toList());
    }

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count != null ? count : 0L;
    }

    private int referenceCount(Long skillId) {
        Integer studentSkillCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_skills WHERE skill_id = ?",
                Integer.class,
                skillId);
        Integer jdSkillCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_description_skills WHERE skill_id = ?",
                Integer.class,
                skillId);
        return (studentSkillCount != null ? studentSkillCount : 0)
                + (jdSkillCount != null ? jdSkillCount : 0);
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
