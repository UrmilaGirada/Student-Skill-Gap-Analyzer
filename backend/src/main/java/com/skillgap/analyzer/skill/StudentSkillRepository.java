package com.skillgap.analyzer.skill;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for {@link StudentSkill}.
 */
@Repository
public interface StudentSkillRepository extends JpaRepository<StudentSkill, Long> {

    /** All skills attached to one student. */
    List<StudentSkill> findByStudentId(Long studentId);

    /** Cheap existence check used to reject duplicates before hitting the unique constraint. */
    boolean existsByStudentIdAndSkillId(Long studentId, Long skillId);

    /** Single student + skill pair, used when removing a skill from a student. */
    Optional<StudentSkill> findByStudentIdAndSkillId(Long studentId, Long skillId);
}
