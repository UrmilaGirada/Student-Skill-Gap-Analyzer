package com.skillgap.analyzer.job;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for {@link JobDescription}.
 */
@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {

    /** All job descriptions of one student, newest first. */
    List<JobDescription> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /** A single job description, but only when it belongs to the given student. */
    Optional<JobDescription> findByIdAndStudentId(Long id, Long studentId);
}
