package com.skillgap.analyzer.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for {@link StudentProfile}.
 */
@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
}
