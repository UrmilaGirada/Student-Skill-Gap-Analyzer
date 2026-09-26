package com.skillgap.analyzer.skill;

import com.skillgap.analyzer.student.StudentProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

/**
 * Links one {@link StudentProfile} to one {@link Skill}, together with how well the student knows it.
 *
 * <p>A student can hold a given skill only once - enforced both here (unique constraint on
 * {@code student_id + skill_id}) and in the service layer.</p>
 */
@Entity
@Table(name = "student_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_skills_student_skill",
                columnNames = { "student_id", "skill_id" }))
@Check(constraints = "years_of_experience >= 0")
public class StudentSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_student_skills_student"))
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_student_skills_skill"))
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency", nullable = false, length = 20)
    private ProficiencyLevel proficiency;

    /** Years of hands-on experience with the skill; never negative. */
    @Column(name = "years_of_experience", nullable = false)
    private Double yearsOfExperience;

    /** Required by JPA. */
    protected StudentSkill() {
    }

    public StudentSkill(StudentProfile student, Skill skill, ProficiencyLevel proficiency,
                        Double yearsOfExperience) {
        this.student = student;
        this.skill = skill;
        this.proficiency = proficiency;
        this.yearsOfExperience = yearsOfExperience;
    }

    public Long getId() {
        return id;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public Skill getSkill() {
        return skill;
    }

    public ProficiencyLevel getProficiency() {
        return proficiency;
    }

    public void setProficiency(ProficiencyLevel proficiency) {
        this.proficiency = proficiency;
    }

    public Double getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Double yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }
}
