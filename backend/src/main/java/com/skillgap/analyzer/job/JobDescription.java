package com.skillgap.analyzer.job;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.skillgap.analyzer.skill.Skill;
import com.skillgap.analyzer.student.StudentProfile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A target job description of one student, together with the canonical skills the Python AI engine
 * recognised in its text.
 *
 * <p>The required skills reference the shared {@link Skill} catalog through the join table
 * {@code job_description_skills}; the same skill can be attached to a job description only once
 * (unique constraint on {@code job_description_id + skill_id}). Skill rows are shared with the
 * student-skill domain and are therefore never removed here.</p>
 *
 * <p>Phase 7A stores the target and its required skills only - the matched / partially matched /
 * missing comparison and the learning roadmap belong to later phases.</p>
 */
@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_job_descriptions_student"))
    private StudentProfile student;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    /** The plain job-description text the AI engine extracts the required skills from. */
    @Lob
    @Column(name = "description_text", nullable = false, columnDefinition = "LONGTEXT")
    private String descriptionText;

    /**
     * Canonical skills required by this job description.
     *
     * <p>PERSIST is cascaded so a skill the catalog does not know yet is inserted together with the
     * job description in a single transaction. Insertion order is kept, so the API returns the skills
     * in the order the engine detected them.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "job_description_skills",
            joinColumns = @JoinColumn(name = "job_description_id", nullable = false,
                    foreignKey = @ForeignKey(name = "fk_job_description_skills_job_description")),
            inverseJoinColumns = @JoinColumn(name = "skill_id", nullable = false,
                    foreignKey = @ForeignKey(name = "fk_job_description_skills_skill")),
            uniqueConstraints = @UniqueConstraint(name = "uk_job_description_skills_job_skill",
                    columnNames = { "job_description_id", "skill_id" }))
    private Set<Skill> requiredSkills = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected JobDescription() {
    }

    public JobDescription(StudentProfile student, String title, String companyName, String descriptionText) {
        this.student = student;
        this.title = title;
        this.companyName = companyName;
        this.descriptionText = descriptionText;
    }

    /** Assigned automatically on insert. */
    @PrePersist
    void assignTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = Instant.now();
    }

    /** Attaches a required skill; duplicates are ignored by the set itself. */
    public void addRequiredSkill(Skill skill) {
        requiredSkills.add(skill);
    }

    /** Detaches all required skills (their join rows), leaving the shared {@link Skill} rows intact. */
    public void clearRequiredSkills() {
        requiredSkills.clear();
    }

    public Long getId() {
        return id;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public String getTitle() {
        return title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    public Set<Skill> getRequiredSkills() {
        return requiredSkills;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
