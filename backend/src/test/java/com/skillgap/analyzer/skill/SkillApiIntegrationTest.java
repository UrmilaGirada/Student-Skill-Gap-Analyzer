package com.skillgap.analyzer.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@code /api/skills} against the real MySQL database (no mocks, no mocked repositories).
 *
 * <p>Skill names carry a random suffix so the tests can be re-run without leftovers blocking them,
 * and every created row is deleted again in {@link #deleteCreatedSkills()}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SkillApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> createdSkillIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedSkills() {
        createdSkillIds.forEach(skillRepository::deleteById);
        createdSkillIds.clear();
    }

    @Test
    void createsSkillAndRetrievesItByIdAndInTheList() throws Exception {
        String name = uniqueName("Python");

        String body = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson(name, "PROGRAMMING_LANGUAGE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.category").value("PROGRAMMING_LANGUAGE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(body);
        long skillId = created.get("id").asLong();
        createdSkillIds.add(skillId);
        assertThat(skillId).isPositive();

        // the row really is in MySQL
        assertThat(skillRepository.findById(skillId)).isPresent();

        mockMvc.perform(get("/api/skills/{id}", skillId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.category").value("PROGRAMMING_LANGUAGE"));

        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    void rejectsDuplicateSkillNameWithConflict() throws Exception {
        String name = uniqueName("Python");

        String body = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson(name, "PROGRAMMING_LANGUAGE")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        createdSkillIds.add(objectMapper.readTree(body).get("id").asLong());

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson(name, "PROGRAMMING_LANGUAGE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Skill already exists: " + name));
    }

    @Test
    void returns404ForUnknownSkillId() throws Exception {
        mockMvc.perform(get("/api/skills/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Skill not found"));
    }

    @Test
    void rejectsInvalidSkillPayloadWith400() throws Exception {
        // blank name
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"category\":\"TOOL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // missing category
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Some Skill\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // unknown enum value
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Some Skill\",\"category\":\"NOT_A_CATEGORY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void databaseEnforcesUniqueSkillName() {
        String name = uniqueName("Docker");
        Skill first = skillRepository.save(new Skill(name, SkillCategory.TOOL));
        createdSkillIds.add(first.getId());

        // bypasses the controller check on purpose - the unique constraint must refuse the duplicate
        assertThatThrownBy(() -> skillRepository.saveAndFlush(new Skill(name, SkillCategory.TOOL)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static String uniqueName(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String skillJson(String name, String category) {
        return "{\"name\":\"" + name + "\",\"category\":\"" + category + "\"}";
    }
}
