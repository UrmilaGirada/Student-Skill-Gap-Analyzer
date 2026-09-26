"""Unit tests for deterministic skill extraction (Phase 5)."""
from models.skill_extractor import extract_skills


def test_basic_extraction_example():
    text = "Experienced in Java, Spring Boot, REST APIs and JS."
    assert extract_skills(text) == ["Java", "Spring Boot", "REST APIs", "JavaScript"]


def test_alias_normalization():
    assert extract_skills("js py postgres") == ["JavaScript", "Python", "PostgreSQL"]


def test_duplicate_removal_across_casings():
    assert extract_skills("Java, java, JAVA and Spring") == ["Java", "Spring Boot"]


def test_case_insensitive_matching():
    assert extract_skills("jAvA aWs and pYtorch") == ["Java", "AWS", "PyTorch"]


def test_multi_word_skills():
    text = "Power BI, Spring Boot and REST APIs"
    assert extract_skills(text) == ["Power BI", "Spring Boot", "REST APIs"]


def test_short_skill_c_not_matched_inside_words():
    skills = extract_skills("JavaScript, CSS and HTML development")
    assert "C" not in skills
    assert skills == ["JavaScript", "CSS", "HTML"]


def test_short_skill_c_matches_standalone():
    assert extract_skills("Learned C and C++") == ["C", "C++"]


def test_short_skill_r_not_matched_everywhere():
    assert extract_skills("A quick fresher and eager worker") == []


def test_short_skill_r_matches_standalone():
    assert extract_skills("Proficient in R and Python") == ["R", "Python"]


def test_empty_input_returns_empty_list():
    assert extract_skills("") == []


def test_blank_input_returns_empty_list():
    assert extract_skills("   \n\t  ") == []


def test_order_follows_input_not_catalog():
    # alphabetical catalog order would list Docker before Linux
    assert extract_skills("I use Linux and Docker") == ["Linux", "Docker"]


def test_spring_bare_maps_to_spring_boot():
    assert extract_skills("Experienced with Spring") == ["Spring Boot"]


def test_spring_calendar_reference_is_ignored():
    assert extract_skills("Graduating Spring 2025") == []
    assert extract_skills("Dean's list, Spring semester") == []


def test_nodejs_and_javascript_are_distinct():
    assert extract_skills("Node.js and JavaScript") == ["Node.js", "JavaScript"]


def test_common_aliases():
    assert extract_skills("gcp, k8s, ci/cd") == ["Google Cloud", "Kubernetes", "CI/CD"]
