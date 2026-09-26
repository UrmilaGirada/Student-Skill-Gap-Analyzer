package com.skillgap.analyzer.ai.dto;

import java.util.List;

/**
 * Payload returned by the Python AI engine: the canonical skills detected in the submitted text.
 *
 * @param skills canonical skill names in first-detection order (Phase 5 contract)
 */
public record SkillExtractionResponse(List<String> skills) {
}
