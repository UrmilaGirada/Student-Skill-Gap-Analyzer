package com.skillgap.analyzer.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple application health endpoint used to verify that the backend is up and serving requests.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthResponse health() {
        return new HealthResponse("UP", "Student Skill Gap Analyzer backend is running");
    }

    /**
     * Response payload for {@code GET /api/health}.
     */
    public record HealthResponse(String status, String message) {
    }
}
