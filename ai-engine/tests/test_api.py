"""API-level tests for the Phase 5 FastAPI service."""
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_reports_running():
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert "AI engine is running" in body["message"]


def test_extract_success_example_payload():
    response = client.post(
        "/api/v1/skills/extract",
        json={"text": "Experienced in Java, Python, Spring Boot and AWS."},
    )
    assert response.status_code == 200
    assert response.json() == {"skills": ["Java", "Python", "Spring Boot", "AWS"]}


def test_extract_missing_text_is_rejected():
    response = client.post("/api/v1/skills/extract", json={})
    assert response.status_code == 422


def test_extract_blank_text_is_rejected():
    response = client.post("/api/v1/skills/extract", json={"text": "   "})
    assert response.status_code == 422


def test_extract_wrong_type_is_rejected():
    response = client.post("/api/v1/skills/extract", json={"text": 123})
    assert response.status_code == 422
