package com.skillgap.analyzer.analysis;

import java.util.List;
import java.util.Map;

/**
 * Deterministic local mapping from a canonical skill name to suggested learning topics (Phase 7C).
 *
 * <p>This is plain application data - <b>no LLM, no external AI API, no course provider and no web
 * scraping</b>. The lists are fixed constants, so the same roadmap is produced for the same input.
 * Lookup keys are compared with {@link SkillGapAnalysisService#normalize(String)}, so matching is
 * case- and whitespace-insensitive.</p>
 *
 * <p>The topics are generic study guidance for a skill, not personalised expert advice. A skill without
 * a curated entry falls back to {@link #GENERIC_TOPICS}.</p>
 */
public final class LearningTopicCatalog {

    /** Topics used when a skill has no curated entry. */
    public static final List<String> GENERIC_TOPICS = List.of(
            "fundamentals",
            "core concepts",
            "practical implementation",
            "common interview questions",
            "mini project");

    private static final Map<String, List<String>> TOPICS = Map.ofEntries(
            Map.entry("java", List.of("OOP", "Collections", "Exception handling", "Streams", "Generics")),
            Map.entry("spring boot", List.of(
                    "Dependency Injection", "REST Controllers", "Spring Data JPA", "Validation", "Exception Handling")),
            Map.entry("rest apis", List.of(
                    "HTTP methods", "REST principles", "JSON", "Status codes", "API design")),
            Map.entry("mysql", List.of(
                    "SELECT / WHERE", "JOINs", "GROUP BY / HAVING", "Subqueries", "Indexes")),
            Map.entry("git", List.of(
                    "clone / add / commit", "branches", "merge", "pull / push", "conflict resolution")),
            Map.entry("aws", List.of("IAM", "EC2", "S3", "VPC", "Cloud basics")),
            Map.entry("python", List.of("syntax", "functions", "collections", "OOP", "modules")),
            Map.entry("html", List.of("semantic HTML", "forms", "accessibility", "page structure")),
            Map.entry("power bi", List.of(
                    "data import", "data cleaning", "relationships", "DAX basics", "dashboards")),
            Map.entry("machine learning", List.of(
                    "supervised learning", "train/test split", "feature engineering", "model evaluation")),
            Map.entry("github", List.of("repositories", "pull requests", "code review", "issues", "actions basics")),
            Map.entry("javascript", List.of("syntax and types", "functions and scope", "DOM basics", "async/await", "modules")),
            Map.entry("sql", List.of("SELECT", "filtering", "aggregation", "joins", "query tuning basics")),
            Map.entry("docker", List.of("images and containers", "Dockerfile", "volumes", "networking", "compose basics")),
            Map.entry("kubernetes", List.of("pods and deployments", "services", "config maps", "scaling", "kubectl basics")),
            Map.entry("react", List.of("components and props", "state", "hooks", "lists and forms", "calling REST APIs")),
            Map.entry("mongodb", List.of("documents and collections", "CRUD", "queries", "aggregation pipeline", "indexes")),
            Map.entry("linux", List.of("file system", "permissions", "processes", "shell basics", "package management")));

    private LearningTopicCatalog() {
    }

    /** Curated topics for a canonical skill name, or {@link #GENERIC_TOPICS} when none is curated. */
    public static List<String> topicsFor(String canonicalSkillName) {
        return TOPICS.getOrDefault(SkillGapAnalysisService.normalize(canonicalSkillName), GENERIC_TOPICS);
    }
}
