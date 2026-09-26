package com.skillgap.analyzer.skill.dto;

import com.skillgap.analyzer.skill.Skill;
import com.skillgap.analyzer.skill.SkillCategory;

/**
 * Response body representing a skill.
 */
public record SkillResponse(Long id, String name, SkillCategory category) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory());
    }
}
