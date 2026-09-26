package com.skillgap.analyzer.resume;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for {@link Resume}.
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    /** All resumes of one student, newest first. */
    List<Resume> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /** A single resume, but only when it belongs to the given student. */
    Optional<Resume> findByIdAndStudentId(Long id, Long studentId);
}
