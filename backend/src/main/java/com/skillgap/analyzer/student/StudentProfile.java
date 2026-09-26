package com.skillgap.analyzer.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A student registered in the Student Skill Gap Analyzer.
 *
 * <p>Phase 2 contains only the master data of a student. Relationships (resume, skills,
 * certifications, projects, job applications) are intentionally added in later phases.</p>
 */
@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    /** Unique - one account per e-mail address. */
    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "college", length = 180)
    private String college;

    @Column(name = "branch", length = 120)
    private String branch;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "cgpa")
    private Double cgpa;

    /** Required by JPA. */
    protected StudentProfile() {
    }

    public StudentProfile(String fullName, String email, String college, String branch,
                          Integer graduationYear, Double cgpa) {
        this.fullName = fullName;
        this.email = email;
        this.college = college;
        this.branch = branch;
        this.graduationYear = graduationYear;
        this.cgpa = cgpa;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }
}
