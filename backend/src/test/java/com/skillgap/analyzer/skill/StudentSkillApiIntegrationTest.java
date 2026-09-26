package com.skillgap.analyzer.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@code /api/students/{studentId}/skills} against the real MySQL database.
 *
 * <p>A student and a skill are created for each test and everything is removed again afterwards,
 * so no test data is left behind.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentSkillApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private StudentSkillRepository studentSkillRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private StudentProfile student;
    private Skill skill;

    @BeforeEach
    void createStudentAndSkill() {
        student = studentProfileRepository.save(new StudentProfile(
                "Phase Three Test Student",
                "phase3." + uniqueSuffix() + "@skillgap.local",
                "Test College",
                "Computer Science",
                2026,
                8.0));
        skill = skillRepository.save(new Skill("Python-" + uniqueSuffix(), SkillCategory.PROGRAMMING_LANGUAGE));
    }

    @AfterEach
    void removeTestData() {
        studentSkillRepository.deleteAll(studentSkillRepository.findByStudentId(student.getId()));
        skillRepository.deleteById(skill.getId());
        studentProfileRepository.deleteById(student.getId());
    }

    @Test
    void attachesSkillToStudentAndListsIt() throws Exception {
        mockMvc.perform(post("/api/students/{studentId}/skills", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addSkillJson(skill.getId(), "INTERMEDIATE", "1.5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skill.name").value(skill.getName()))
                .andExpect(jsonPath("$.skill.category").value("PROGRAMMING_LANGUAGE"))
                .andExpect(jsonPath("$.proficiency").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.yearsOfExperience").value(1.5));

        // the relationship really is in MySQL
        assertThat(studentSkillRepository.existsByStudentIdAndSkillId(student.getId(), skill.getId())).isTrue();

        String listBody = mockMvc.perform(get("/api/students/{studentId}/skills", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skill.name").value(skill.getName()))
                .andExpect(jsonPath("$[0].proficiency").value("INTERMEDIATE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode list = objectMapper.readTree(listBody);
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).get("studentId").asLong()).isEqualTo(student.getId());
        assertThat(list.get(0).get("skill").get("id").asLong()).isEqualTo(skill.getId());
        assertThat(list.get(0).get("yearsOfExperience").asDouble()).isEqualTo(1.5);
    }

    @Test
    void rejectsDuplicateStudentSkillWithConflict() throws Exception {
        studentSkillRepository.save(new StudentSkill(student, skill, ProficiencyLevel.BEGINNER, 0.5));

        mockMvc.perform(post("/api/students/{studentId}/skills", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addSkillJson(skill.getId(), "INTERMEDIATE", "1.5")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Student already has this skill"));
    }

    @Test
    void returns404WhenStudentDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/students/{studentId}/skills", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addSkillJson(skill.getId(), "BEGINNER", "1.0")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found"));

        mockMvc.perform(get("/api/students/{studentId}/skills", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found"));
    }

    @Test
    void returns404WhenSkillDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/students/{studentId}/skills", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addSkillJson(999_999L, "BEGINNER", "1.0")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Skill not found"));
    }

    @Test
    void removesSkillFromStudentAndThenReturns404() throws Exception {
        studentSkillRepository.save(new StudentSkill(student, skill, ProficiencyLevel.ADVANCED, 3.0));

        mockMvc.perform(delete("/api/students/{studentId}/skills/{skillId}", student.getId(), skill.getId()))
                .andExpect(status().isNoContent());

        assertThat(studentSkillRepository.existsByStudentIdAndSkillId(student.getId(), skill.getId())).isFalse();
        assertThat(studentSkillRepository.findByStudentId(student.getId())).isEmpty();

        mockMvc.perform(delete("/api/students/{studentId}/skills/{skillId}", student.getId(), skill.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student skill relationship not found"));
    }

    @Test
    void rejectsInvalidStudentSkillPayloadWith400() throws Exception {
        // negative years of experience
        mockMvc.perform(post("/api/students/{studentId}/skills", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addSkillJson(skill.getId(), "BEGINNER", "-1.0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // missing proficiency
        mockMvc.perform(post("/api/students/{studentId}/skills", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":" + skill.getId() + ",\"yearsOfExperience\":1.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // missing skillId
        mockMvc.perform(post("/api/students/{studentId}/skills", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proficiency\":\"BEGINNER\",\"yearsOfExperience\":1.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void databaseEnforcesUniqueStudentSkillPair() {
        studentSkillRepository.save(new StudentSkill(student, skill, ProficiencyLevel.BEGINNER, 0.5));

        // bypasses the service check on purpose - the composite unique constraint must refuse it
        assertThatThrownBy(() -> studentSkillRepository.saveAndFlush(
                new StudentSkill(student, skill, ProficiencyLevel.EXPERT, 5.0)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(studentSkillRepository.findByStudentId(student.getId())).hasSize(1);
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String addSkillJson(Long skillId, String proficiency, String yearsOfExperience) {
        return "{\"skillId\":" + skillId + ",\"proficiency\":\"" + proficiency + "\","
                + "\"yearsOfExperience\":" + yearsOfExperience + "}";
    }
}
