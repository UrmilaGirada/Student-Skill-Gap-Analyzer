package com.skillgap.analyzer.controller;

import java.util.List;

import com.skillgap.analyzer.exception.DuplicateResourceException;
import com.skillgap.analyzer.exception.ResourceNotFoundException;
import com.skillgap.analyzer.skill.Skill;
import com.skillgap.analyzer.skill.SkillRepository;
import com.skillgap.analyzer.skill.dto.CreateSkillRequest;
import com.skillgap.analyzer.skill.dto.SkillResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD-ish REST API for the skill catalog ({@code /api/skills}).
 *
 * <p>Plain persistence access only - there is no business logic beyond preventing duplicate names,
 * so no service layer is required here.</p>
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillRepository skillRepository;

    public SkillController(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse createSkill(@Valid @RequestBody CreateSkillRequest request) {
        String name = request.name().trim();

        if (skillRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new DuplicateResourceException("Skill already exists: " + name);
        }

        Skill saved = skillRepository.save(new Skill(name, request.category()));
        return SkillResponse.from(saved);
    }

    @GetMapping
    public List<SkillResponse> findAllSkills() {
        return skillRepository.findAll().stream()
                .map(SkillResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SkillResponse findSkillById(@PathVariable Long id) {
        return skillRepository.findById(id)
                .map(SkillResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
    }
}
