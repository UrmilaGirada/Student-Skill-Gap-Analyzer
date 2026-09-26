"""HTTP endpoints of the AI engine (Phase 5).

Endpoints
---------
GET  /health                  - liveness check for the Spring Boot backend / operators.
POST /api/v1/skills/extract   - extract canonical skills from free text.
"""
from fastapi import APIRouter
from pydantic import BaseModel, Field, field_validator

from models.skill_extractor import extract_skills

router = APIRouter()


class SkillExtractRequest(BaseModel):
    """Payload for POST /api/v1/skills/extract."""

    text: str = Field(..., description="Raw resume or job-description text.")

    @field_validator("text")
    @classmethod
    def text_must_not_be_blank(cls, value: str) -> str:
        if not value or not value.strip():
            raise ValueError("text must not be blank")
        return value


class SkillExtractResponse(BaseModel):
    """Canonical skills detected in the submitted text."""

    skills: list[str]


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "message": "Student Skill Gap Analyzer AI engine is running"}


@router.post("/api/v1/skills/extract", response_model=SkillExtractResponse)
def extract(payload: SkillExtractRequest) -> SkillExtractResponse:
    return SkillExtractResponse(skills=extract_skills(payload.text))
