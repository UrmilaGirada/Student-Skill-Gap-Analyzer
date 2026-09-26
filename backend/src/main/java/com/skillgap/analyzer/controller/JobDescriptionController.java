package com.skillgap.analyzer.controller;

import java.util.List;

import com.skillgap.analyzer.analysis.SkillGapAnalysisService;
import com.skillgap.analyzer.analysis.SkillGapRoadmapService;
import com.skillgap.analyzer.analysis.dto.SkillGapAnalysisResponse;
import com.skillgap.analyzer.analysis.dto.SkillGapRoadmapResponse;
import com.skillgap.analyzer.job.JobDescriptionService;
import com.skillgap.analyzer.job.dto.JobDescriptionRequest;
import com.skillgap.analyzer.job.dto.JobDescriptionResponse;

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
 * REST API for job descriptions ({@code /api/students/{studentId}/job-descriptions}).
 *
 * <p>Phase 7A stores a target job description and the canonical skills the Python AI engine extracts
 * from it; Phase 7B adds the read-only skill-gap analysis against the student's stored skills.</p>
 */
@RestController
@RequestMapping("/api/students/{studentId}/job-descriptions")
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;
    private final SkillGapAnalysisService skillGapAnalysisService;
    private final SkillGapRoadmapService skillGapRoadmapService;

    public JobDescriptionController(JobDescriptionService jobDescriptionService,
                                    SkillGapAnalysisService skillGapAnalysisService,
                                    SkillGapRoadmapService skillGapRoadmapService) {
        this.jobDescriptionService = jobDescriptionService;
        this.skillGapAnalysisService = skillGapAnalysisService;
        this.skillGapRoadmapService = skillGapRoadmapService;
    }

    /** Stores a job description and extracts its required skills through the existing AI engine. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobDescriptionResponse createJobDescription(@PathVariable Long studentId,
                                                       @Valid @RequestBody JobDescriptionRequest request) {
        return jobDescriptionService.createJobDescription(studentId, request);
    }

    @GetMapping
    public List<JobDescriptionResponse> findJobDescriptionsOfStudent(@PathVariable Long studentId) {
        return jobDescriptionService.findJobDescriptionsOfStudent(studentId);
    }

    @GetMapping("/{jobDescriptionId}")
    public JobDescriptionResponse findJobDescription(@PathVariable Long studentId,
                                                     @PathVariable Long jobDescriptionId) {
        return jobDescriptionService.findJobDescription(studentId, jobDescriptionId);
    }

    @DeleteMapping("/{jobDescriptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJobDescription(@PathVariable Long studentId, @PathVariable Long jobDescriptionId) {
        jobDescriptionService.deleteJobDescription(studentId, jobDescriptionId);
    }

    /**
     * Phase 7B: read-only skill-gap analysis of this job description against the student's stored
     * skills. The comparison itself lives in {@link SkillGapAnalysisService}.
     */
    @GetMapping("/{jobDescriptionId}/skill-gap")
    public SkillGapAnalysisResponse findSkillGap(@PathVariable Long studentId,
                                                 @PathVariable Long jobDescriptionId) {
        return skillGapAnalysisService.analyseSkillGap(studentId, jobDescriptionId);
    }

    /**
     * Phase 7C: deterministic learning roadmap for this job description, built on top of the Phase 7B
     * analysis. Read-only - nothing is persisted.
     */
    @GetMapping("/{jobDescriptionId}/roadmap")
    public SkillGapRoadmapResponse findRoadmap(@PathVariable Long studentId,
                                               @PathVariable Long jobDescriptionId) {
        return skillGapRoadmapService.buildRoadmap(studentId, jobDescriptionId);
    }
}
