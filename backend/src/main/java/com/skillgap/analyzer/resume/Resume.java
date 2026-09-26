package com.skillgap.analyzer.resume;

import java.time.Instant;

import com.skillgap.analyzer.student.StudentProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Metadata plus extracted plain text of an uploaded resume.
 *
 * <p>The original PDF/DOCX binary is deliberately not stored: only the file metadata and the
 * extracted text are persisted, so later phases can work on the text without touching binaries.</p>
 */
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_resumes_student"))
    private StudentProfile student;

    /** Client-supplied file name, sanitized (never a filesystem path). */
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    /** Uploaded size in bytes. */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /** Raw text extracted from the document - the input of later phases. */
    @Lob
    @Column(name = "extracted_text", nullable = false, columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected Resume() {
    }

    public Resume(StudentProfile student, String originalFileName, String contentType,
                  long fileSize, String extractedText) {
        this.student = student;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.extractedText = extractedText;
    }

    /** Assigned automatically on insert. */
    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
