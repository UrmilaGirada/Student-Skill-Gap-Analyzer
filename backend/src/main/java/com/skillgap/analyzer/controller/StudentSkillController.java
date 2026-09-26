package com.skillgap.analyzer.controller;

import java.util.List;

import com.skillgap.analyzer.skill.StudentSkillService;
import com.skillgap.analyzer.skill.dto.AddStudentSkillRequest;
import com.skillgap.analyzer.skill.dto.StudentSkillResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the skills a student holds ({@code /api/students/{studentId}/skills}).
 */
@RestController
@RequestMapping("/api/students/{studentId}/skills")
public class StudentSkillController {

    private final StudentSkillService studentSkillService;

    public StudentSkillController(StudentSkillService studentSkillService) {
        this.studentSkillService = studentSkillService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentSkillResponse addSkillToStudent(@PathVariable Long studentId,
                                                  @Valid @RequestBody AddStudentSkillRequest request) {
        return studentSkillService.addSkill(studentId, request);
    }

    @GetMapping
    public List<StudentSkillResponse> findSkillsOfStudent(@PathVariable Long studentId) {
        return studentSkillService.findSkillsOfStudent(studentId);
    }

    @DeleteMapping("/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSkillFromStudent(@PathVariable Long studentId, @PathVariable Long skillId) {
        studentSkillService.removeSkill(studentId, skillId);
    }
}
