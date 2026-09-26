package com.skillgap.analyzer.skill.dto;

import com.skillgap.analyzer.skill.ProficiencyLevel;
import com.skillgap.analyzer.skill.StudentSkill;

/**
 * Response body representing one skill held by a student.
 */
public record StudentSkillResponse(
        Long id,
        Long studentId,
        SkillResponse skill,
        ProficiencyLevel proficiency,
        Double yearsOfExperience) {

    public static StudentSkillResponse from(StudentSkill studentSkill) {
        return new StudentSkillResponse(
                studentSkill.getId(),
                studentSkill.getStudent().getId(),
                SkillResponse.from(studentSkill.getSkill()),
                studentSkill.getProficiency(),
                studentSkill.getYearsOfExperience());
    }
}
