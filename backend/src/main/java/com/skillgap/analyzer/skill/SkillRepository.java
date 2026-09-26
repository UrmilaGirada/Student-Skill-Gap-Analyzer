package com.skillgap.analyzer.skill;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for {@link Skill}.
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    /**
     * Case-insensitive lookup by name, used to prevent duplicate skills
     * (for example {@code Python} vs {@code python}).
     */
    Optional<Skill> findByNameIgnoreCase(String name);
}
