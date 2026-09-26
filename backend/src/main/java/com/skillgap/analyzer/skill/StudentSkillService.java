package com.skillgap.analyzer.skill;

import java.util.List;

import com.skillgap.analyzer.exception.DuplicateResourceException;
import com.skillgap.analyzer.exception.ResourceNotFoundException;
import com.skillgap.analyzer.skill.dto.AddStudentSkillRequest;
import com.skillgap.analyzer.skill.dto.StudentSkillResponse;
import com.skillgap.analyzer.student.StudentProfile;
import com.skillgap.analyzer.student.StudentProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for attaching skills to students: it verifies both sides of the relationship
 * exist and that a student does not hold the same skill twice.
 */
@Service
public class StudentSkillService {

    private final StudentProfileRepository studentProfileRepository;
    private final SkillRepository skillRepository;
    private final StudentSkillRepository studentSkillRepository;

    public StudentSkillService(StudentProfileRepository studentProfileRepository,
                               SkillRepository skillRepository,
                               StudentSkillRepository studentSkillRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.skillRepository = skillRepository;
        this.studentSkillRepository = studentSkillRepository;
    }

    /**
     * Attaches a skill to a student.
     *
     * @throws ResourceNotFoundException   if the student or the skill does not exist
     * @throws DuplicateResourceException  if the student already holds that skill
     */
    public StudentSkillResponse addSkill(Long studentId, AddStudentSkillRequest request) {
        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));

        if (studentSkillRepository.existsByStudentIdAndSkillId(studentId, skill.getId())) {
            throw new DuplicateResourceException("Student already has this skill");
        }

        // The unique constraint on student_id + skill_id is the final guard against duplicates.
        StudentSkill saved = studentSkillRepository.save(
                new StudentSkill(student, skill, request.proficiency(), request.yearsOfExperience()));

        return StudentSkillResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentSkillResponse> findSkillsOfStudent(Long studentId) {
        if (!studentProfileRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found");
        }
        return studentSkillRepository.findByStudentId(studentId).stream()
                .map(StudentSkillResponse::from)
                .toList();
    }

    /**
     * Removes a skill from a student.
     *
     * @throws ResourceNotFoundException if the student does not hold that skill
     */
    public void removeSkill(Long studentId, Long skillId) {
        StudentSkill existing = studentSkillRepository.findByStudentIdAndSkillId(studentId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Student skill relationship not found"));

        studentSkillRepository.delete(existing);
    }
}
