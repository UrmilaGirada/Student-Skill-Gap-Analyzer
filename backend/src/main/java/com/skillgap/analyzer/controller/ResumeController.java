package com.skillgap.analyzer.controller;

import java.util.List;

import com.skillgap.analyzer.ai.AiEngineService;
import com.skillgap.analyzer.ai.dto.ExtractSkillsResponse;
import com.skillgap.analyzer.resume.ResumeService;
import com.skillgap.analyzer.resume.dto.ResumeResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST API for resume upload and retrieval ({@code /api/students/{studentId}/resumes}).
 *
 * <p>Uploads and reads are Phase 4 document ingestion; {@code /extract-skills} is the Phase 6
 * bridge to the Python AI engine. Full skill-gap analysis still belongs to later phases.</p>
 */
@RestController
@RequestMapping("/api/students/{studentId}/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final AiEngineService aiEngineService;

    public ResumeController(ResumeService resumeService, AiEngineService aiEngineService) {
        this.resumeService = resumeService;
        this.aiEngineService = aiEngineService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse uploadResume(@PathVariable Long studentId,
                                       @RequestParam("file") MultipartFile file) {
        return resumeService.storeResume(studentId, file);
    }

    @GetMapping
    public List<ResumeResponse> findResumesOfStudent(@PathVariable Long studentId) {
        return resumeService.findResumesOfStudent(studentId);
    }

    @GetMapping("/{resumeId}")
    public ResumeResponse findResume(@PathVariable Long studentId, @PathVariable Long resumeId) {
        return resumeService.findResume(studentId, resumeId);
    }

    @DeleteMapping("/{resumeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResume(@PathVariable Long studentId, @PathVariable Long resumeId) {
        resumeService.deleteResume(studentId, resumeId);
    }

    /**
     * Phase 6: sends the stored resume text to the Python AI engine and returns the canonical
     * skills it recognises. Nothing is persisted at this stage.
     */
    @PostMapping("/{resumeId}/extract-skills")
    public ExtractSkillsResponse extractSkills(@PathVariable Long studentId, @PathVariable Long resumeId) {
        return aiEngineService.extractSkillsFromResume(studentId, resumeId);
    }
}
