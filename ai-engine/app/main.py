"""FastAPI entry point for the Student Skill Gap Analyzer AI engine.

Run from the ``ai-engine`` directory:

    uvicorn app.main:app --reload
"""
from fastapi import FastAPI

from app.api.routes import router

app = FastAPI(
    title="Student Skill Gap Analyzer - AI Engine",
    description="Phase 5: deterministic skill extraction and canonical normalization.",
    version="0.1.0",
)

app.include_router(router)
