# ai-engine

Python service for the **Student Skill Gap Analyzer** NLP/AI logic.

## Status

**Phase 5 - skill extraction foundation (implemented) - consumed by Spring Boot since Phase 6,
integration verified end-to-end.**

This phase delivers a deterministic, dependency-light FastAPI service that turns free text
(resume or job-description prose) into normalized canonical skill names. It is deliberately
rule-based: no LLMs, no ML frameworks.

## What Phase 5 implements

- `data/skills.json` - canonical skill catalog (44 entries) with aliases/synonyms
  (`js -> JavaScript`, `postgres -> PostgreSQL`, `gcp -> Google Cloud`, ...).
- `models/skill_extractor.py` - deterministic extraction: word-safe matching
  (short skills like `C`/`R` never match inside other words), case-insensitive
  alias normalization, duplicate removal, first-detection ordering.
- `app/` - FastAPI service with `GET /health` and `POST /api/v1/skills/extract`.
- `tests/` - pytest suite (16 extractor tests + 5 API tests).

### Canonical skill normalization

Every mention in the catalog (canonical name + aliases) maps to exactly ONE canonical label.
Extraction rules:

1. **Word-safe matching** - terms use `(?<!\w)`/`(?!\w)` guards, so `C` does not match in
   `city`/`CSS`, and `C++`/`C#` still match correctly (plain `\b` would fail after `+`).
2. **Longest-first** - `JavaScript` beats `java`/`js`; `C++` beats `C` at the same position.
3. **Case-insensitive** - `js`, `JS`, `Js` all normalize to `JavaScript`.
4. **Duplicates collapse** - `Java, java, JAVA` -> `["Java"]`.
5. **Deterministic order** - skills are returned in the order they are FIRST detected in the
   input (left to right), never catalog order.
6. **Calendar guard** - bare `Spring` maps to `Spring Boot`, except before a year / `semester`
   / `term` (`Graduating Spring 2025` extracts nothing).

## Getting started (Python 3.11)

```powershell
cd ai-engine
python -m venv .venv
.\.venv\Scripts\Activate.ps1     # optional; you can also call .\.venv\Scripts\python.exe directly
python -m pip install -r requirements.txt
```

Dependencies: `fastapi`, `uvicorn`, `pytest`, `httpx` - nothing else.

## Run the tests

```powershell
python -m pytest -q
```

## Start the service

```powershell
python -m uvicorn app.main:app --reload      # dev reload on http://127.0.0.1:8000
```

## Endpoint examples

```http
GET /health
{"status": "UP", "message": "Student Skill Gap Analyzer AI engine is running"}

POST /api/v1/skills/extract
{"text": "Experienced in Java, Python, Spring Boot and AWS."}

200 OK
{"skills": ["Java", "Python", "Spring Boot", "AWS"]}
```

Validation: missing / blank / non-string `text` -> `422` with a FastAPI validation detail.

## Integration contract (Phase 6)

The Spring Boot backend consumes this service through `AiEngineClient` (Spring `RestClient`):

- Base URL `http://localhost:8000`, overridable with the `AI_ENGINE_BASE_URL` environment variable
- Call: `POST /api/v1/skills/extract` with `{"text": "<stored resume text>"}`
- Response: `{"skills": ["Java", "Spring Boot", ...]}` - the unchanged Phase 5 contract
- Failures are handled Spring-side: engine down/timeout → HTTP 503, engine error/invalid
  payload → HTTP 502 (no Python internals are ever forwarded)

Verified end-to-end (Phase 6): backend `mvn clean test` 44/44 green, Python suite 21 passed, and a
live call (`POST /api/students/166/resumes/65/extract-skills`) returned the canonical skills for a
stored PDF resume - including this service being reported as unavailable (`503`) once stopped.
The request/response contract above is unchanged and is the stable interface for later phases; no
Phase 5 extractor logic, catalog entry or test was modified to achieve this.

## NOT implemented yet (later phases)
- Resume/job-description document parsing (PDF/DOCX handling lives in the backend today)
- Skill matching / gap analysis (matched / partial / missing classification)
- Learning roadmap generation
- Real NLP/ML or LLM-based extraction (may augment the rule-based engine later)

## Related folders

- `../backend` - Spring Boot backend that will orchestrate requests and call this engine.
- `../docs` - architecture notes documenting the request/response contracts between the services.

