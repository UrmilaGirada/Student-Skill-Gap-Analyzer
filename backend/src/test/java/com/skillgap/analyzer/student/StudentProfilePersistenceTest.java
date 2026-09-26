package com.skillgap.analyzer.student;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test for the Phase 2 persistence foundation.
 *
 * <p>This test uses the real MySQL database configured in
 * {@code application.properties} - no mocked repository - so a green run proves that
 * Spring Boot can actually connect to MySQL and persist/read a {@link StudentProfile}.
 * The inserted test record is deleted again in {@link #deleteTestRecord()}.</p>
 */
@SpringBootTest
class StudentProfilePersistenceTest {

    private static final String TEST_EMAIL = "phase2.persistence.test@skillgap.local";

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private DataSource dataSource;

    private Long createdId;

    @AfterEach
    void deleteTestRecord() {
        if (createdId != null) {
            studentProfileRepository.deleteById(createdId);
            createdId = null;
        }
    }

    @Test
    void connectsToTheConfiguredMySqlDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).containsIgnoringCase("MySQL");
            assertThat(connection.getCatalog()).isEqualTo("ai_skill_gap_db");
            assertThat(connection.isValid(5)).isTrue();
        }
    }

    @Test
    void savesReadsAndDeletesStudentProfile() {
        StudentProfile profile = new StudentProfile(
                "Phase Two Test Student",
                TEST_EMAIL,
                "Government Engineering College",
                "Computer Science",
                2026,
                8.5);

        // 1. INSERT into MySQL
        StudentProfile saved = studentProfileRepository.save(profile);
        createdId = saved.getId();
        assertThat(createdId).isNotNull().isPositive();

        // 2. SELECT back from MySQL
        StudentProfile reloaded = studentProfileRepository.findById(createdId).orElseThrow();

        // 3. verify the round trip
        assertThat(reloaded.getId()).isEqualTo(createdId);
        assertThat(reloaded.getFullName()).isEqualTo("Phase Two Test Student");
        assertThat(reloaded.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(reloaded.getCollege()).isEqualTo("Government Engineering College");
        assertThat(reloaded.getBranch()).isEqualTo("Computer Science");
        assertThat(reloaded.getGraduationYear()).isEqualTo(2026);
        assertThat(reloaded.getCgpa()).isEqualTo(8.5);

        // 4. cleanup (also verified here so the assertion runs against MySQL)
        studentProfileRepository.deleteById(createdId);
        assertThat(studentProfileRepository.findById(createdId)).isEmpty();
        createdId = null;
    }
}
