package com.skillgap.analyzer.ai.dto;

/**
 * Payload sent to the Python AI engine: {@code POST /api/v1/skills/extract}.
 *
 * @param text raw resume text stored by the backend
 */
public record SkillExtractionRequest(String text) {
}
