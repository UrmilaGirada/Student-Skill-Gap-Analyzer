# Student Skill Gap Analyzer

A full-stack application that compares a student's resume against a target job description, highlights the
skill gaps, and produces a personalized learning roadmap to close them.

---

## 1. Project name

**Student Skill Gap Analyzer**

## 2. Real-world problem

Students and fresh graduates often apply for jobs without knowing how well their current skills match the
requirements of the role. Job descriptions list dozens of tools, technologies, and competencies, and a
resume rarely maps to them in an obvious way. The result is guesswork: applicants study random topics,
miss the skills recruiters actually ask for, and receive little or no actionable feedback. Manually
comparing a resume with a job description is slow, subjective, and inconsistent.

## 3. Project goal

Build a system where a student can:

1. Upload a resume (PDF/DOCX) and paste a job description.
2. Have skills extracted from both documents automatically.
3. Receive a clear comparison of **matched**, **missing**, and **partially matched** skills.
4. Get a personalized, prioritized skill-gap learning roadmap showing what to learn next and why.

The final product is meant to be a practical career tool, not just a document parser: the comparison
result is what drives the recommendation.

## 4. Planned technology stack

| Layer | Technology | Responsibility |
| --- | --- | --- |
| Frontend | React | Upload flows and skill-gap / roadmap dashboards |
| Backend | Spring Boot (Java 21) | REST API, orchestration, business rules, persistence |
| AI / NLP engine | Python | Resume + job description parsing, skill extraction, gap analysis |
| Database | MySQL 8 | Student profiles, documents, skill catalog, analysis results |
| Security (later) | JWT | Stateless authentication and authorization |
| Deployment (later) | AWS | Hosting, storage, and managed database |

## 5. Current development phase

**Phase 7C - Deterministic learning roadmap and recommendations (implemented).**

Phase 1 delivered the project structure and a runnable Spring Boot backend (Spring Web + Actuator +
`GET /api/health`). Phase 2 added the database foundation (MySQL 8 + JPA + `StudentProfile`).
Phase 3 added the core skill domain (skill catalog, student ↔ skill relationship, REST APIs).
Phase 4 implemented document ingestion (PDF/DOCX upload, validation, Apache Tika, `resumes` storage).
Phase 5 added the Python engine foundation: a FastAPI service that extracts and normalizes canonical
skills from free text (see `ai-engine/README.md`). Phase 6 connects the two services: Spring Boot now
sends stored resume text to the Python engine and returns the recognized skills. Phase 7A opens the
job-description side: a student can store a target job description whose required skills are extracted
by the same engine and kept for the later skill-gap comparison. Phase 7B adds that comparison: the
stored resume skills of a student are matched against a stored job description's required skills and
classified as matched, partially matched or missing, with a transparent match percentage.
Phase 7C turns that classification into a deterministic learning roadmap with prioritised steps, reasons,
and curated local learning topics.

> **Phases 4-7C cover ingestion, extraction, comparison, and deterministic roadmaps only.
> All roadmap logic is local and rule-based - no external AI APIs, LLMs, or course providers.**

Still intentionally **not** implemented (out of scope for Phase 7C):

- No LLM/AI APIs (OpenAI/Gemini) and no ML/NLP frameworks
- No fuzzy or semantic skill matching (only exact and token-aligned partial matching)
- No persistence of analysis or roadmap results - no `MatchResult` entity and no `match_results` table
- No dynamic course scraping or provider integrations (Coursera, Udemy, YouTube)
- No authentication (JWT) or Spring Security
- No React UI
- No AWS deployment, S3 or Docker

What is in place after Phase 6:

```
Student-Skill-Gap-Analyzer/
├── backend/          # Spring Boot backend: REST + Actuator + JPA/MySQL (ACTIVE)
├── ai-engine/        # Python FastAPI engine: canonical skill extraction (Phase 5)
├── frontend/         # React UI (placeholder)
├── docs/             # Project documentation (placeholder)
└── README.md
```

### Backend quick start

Prerequisites: Java 21, Maven, and a running MySQL 8 server on `localhost:3306`.

**1. Set the database credentials.** They are never stored in the repository - Spring reads them from
environment variables:

```powershell
# current PowerShell session only
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-mysql-password>"

# or persist them for future sessions (reopen the terminal afterwards)
setx DB_USERNAME "root"
setx DB_PASSWORD "<your-mysql-password>"
```

`DB_USERNAME` falls back to `root` when it is not set. `DB_PASSWORD` must be provided - the application
will not connect to MySQL without it.

**2. Build, test and run:**

```bash
cd backend
mvn clean test         # unit tests + the MySQL persistence integration test
mvn spring-boot:run    # starts the backend on http://localhost:8080
```

**3. Verify it is alive:**

```bash
curl http://localhost:8080/api/health        # {"status":"UP","message":"..."}
curl http://localhost:8080/actuator/health   # overall Infrastructure status, includes the db component
```

Expected response for the application health endpoint:

```json
{
  "status": "UP",
  "message": "Student Skill Gap Analyzer backend is running"
}
```

### Database configuration

| Item | Value |
| --- | --- |
| Database | `ai_skill_gap_db` (MySQL 8.x) |
| JDBC URL | `jdbc:mysql://localhost:3306/ai_skill_gap_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| Credentials | environment variables `DB_USERNAME` (defaults to `root`) and `DB_PASSWORD` |
| Schema management | `spring.jpa.hibernate.ddl-auto=update` (never `create` / `create-drop`) |
| SQL logging | `spring.jpa.show-sql=false` |
| Tables | `student_profiles`, `skills`, `student_skills`, `resumes` |

The schema is created automatically on startup: `createDatabaseIfNotExist=true` creates the database on
the first connection and Hibernate creates/updates the tables.

### REST API (Phases 1-7B)

| Method | Endpoint | Purpose | Success | Errors |
| --- | --- | --- | --- | --- |
| GET | `/api/health` | Application health | 200 | - |
| GET | `/actuator/health` | Infrastructure + DB health | 200 | - |
| POST | `/api/skills` | Create a skill | 201 | 400 invalid body, 409 duplicate name |
| GET | `/api/skills` | List all skills | 200 | - |
| GET | `/api/skills/{id}` | Get one skill | 200 | 404 unknown id |
| POST | `/api/students/{studentId}/skills` | Attach a skill to a student | 201 | 400 invalid body, 404 student/skill, 409 already attached |
| GET | `/api/students/{studentId}/skills` | List a student's skills | 200 | 404 unknown student |
| DELETE | `/api/students/{studentId}/skills/{skillId}` | Detach a skill | 204 | 404 not attached |
| POST | `/api/students/{studentId}/resumes` | Upload a resume (multipart field `file`) | 201 | 400 missing file, 404 student, 413 too large, 415 unsupported type, 422 no text |
| GET | `/api/students/{studentId}/resumes` | List a student's resumes | 200 | 404 unknown student |
| GET | `/api/students/{studentId}/resumes/{resumeId}` | Get one resume (incl. extracted text) | 200 | 404 student/resume |
| DELETE | `/api/students/{studentId}/resumes/{resumeId}` | Delete a resume record | 204 | 404 student/resume |
| POST | `/api/students/{studentId}/resumes/{resumeId}/extract-skills` | Extract canonical skills via the Python AI engine (Phase 6) | 200 | 404 student/resume, 422 no text, 502/503 engine error |
| POST | `/api/students/{studentId}/job-descriptions` | Store a job description and extract its required skills (Phase 7A) | 201 | 400 invalid body, 404 unknown student, 502/503 engine error |
| GET | `/api/students/{studentId}/job-descriptions` | List a student's job descriptions | 200 | 404 unknown student |
| GET | `/api/students/{studentId}/job-descriptions/{jobDescriptionId}` | Get one job description with its required skills | 200 | 404 student/job description |
| DELETE | `/api/students/{studentId}/job-descriptions/{jobDescriptionId}` | Delete a job description (shared skills stay) | 204 | 404 student/job description |
| GET | `/api/students/{studentId}/job-descriptions/{jobDescriptionId}/skill-gap` | Read-only skill-gap analysis: matched / partially matched / missing + match percentage (Phase 7B) | 200 | 404 student/job description |
| GET | `/api/students/{studentId}/job-descriptions/{jobDescriptionId}/roadmap` | Read-only deterministic learning roadmap + recommendations (Phase 7C) | 200 | 404 student/job description |

Errors always use the same shape: `{"status": 404, "message": "Student not found"}`.

Example request bodies:

```json
POST /api/skills
{ "name": "Python", "category": "PROGRAMMING_LANGUAGE" }

POST /api/students/1/skills
{ "skillId": 1, "proficiency": "INTERMEDIATE", "yearsOfExperience": 1.5 }
```

Domain model of Phase 3: `StudentProfile` ← `StudentSkill` → `Skill`, where `StudentSkill` stores the
`proficiency` (`BEGINNER` / `INTERMEDIATE` / `ADVANCED` / `EXPERT`) and `yearsOfExperience`.
A student can hold a given skill only once — a composite unique constraint on
`student_id + skill_id` enforces this in the database.

### Phase 4 - Resume upload & text extraction

**Phase 4 extracts resume text only. Skill extraction and NLP are implemented in later phases.**

| Item | Value |
| --- | --- |
| Supported files | PDF (`application/pdf`), DOCX (`application/vnd.openxmlformats-officedocument.wordprocessingml.document`) |
| Max file size | 10 MB per file / 12 MB per request (Spring multipart config **and** service-level validation) |
| Upload | `POST /api/students/{studentId}/resumes` (multipart field `file`) → 201 with metadata + `extractedText` |
| Read | `GET /api/students/{studentId}/resumes` · `GET .../resumes/{resumeId}` → 200, 404 if not found |
| Delete | `DELETE .../resumes/{resumeId}` → 204, 404 if not found |
| What is stored | metadata + extracted text only - the original PDF/DOCX binary is **never** stored in MySQL or on disk |
| Extraction | Apache Tika inside Spring Boot (no Python); deterministic clean-up of line endings and repeated blank lines only |

Error contract: `400` missing file · `404` student/resume not found · `413` larger than 10 MB ·
`415` unsupported or forged file type (real magic bytes must match the declared type) ·
`422` no extractable text (e.g. a scanned image-only PDF - OCR is deliberately out of scope in Phase 4).

Security considerations: the client-supplied file name is sanitized (directory parts and control
characters removed, length capped), uploads are processed in memory and never written to or executed
on the server, only whitelisted content types are accepted, resume access is scoped to the owning
student, and error responses never expose stack traces or server paths.

Phase 4 limitations: no OCR, no skill/keyword extraction, no scoring - a resume is stored as raw text.

Phase 5 roadmap: the Python NLP engine consumes `resumes.extracted_text`, extracts and normalizes
skills, and feeds the skill-gap comparison.

### Phase 6 - Spring Boot ↔ Python AI engine integration

| Item | Value |
| --- | --- |
| Python AI engine | `http://localhost:8000` (FastAPI, Phase 5) |
| Spring Boot backend | `http://localhost:8080` |
| Backend → Python call | `POST {base-url}/api/v1/skills/extract` with `{"text": "<stored resume text>"}` → `{"skills": [...]}` |
| New backend endpoint | `POST /api/students/{studentId}/resumes/{resumeId}/extract-skills` → `{"studentId": 1, "resumeId": 2, "skills": [...]}` |
| Configuration | `ai.engine.base-url=${AI_ENGINE_BASE_URL:http://localhost:8000}` plus 2 s connect / 5 s read timeouts |
| Architecture | Controller → `AiEngineService` → `AiEngineClient` (Spring `RestClient`) → FastAPI |

Error behaviour: `404` student/resume not found (resumes stay scoped to their student) ·
`422` resume has no extracted text · `503` AI engine unreachable (down/refused/timeout) ·
`502` AI engine answered with an error or invalid payload. Stack traces and Python internals are
never exposed. Phase 6 does **not** persist extracted skills (no new tables) and does **not**
implement job-description matching or learning-roadmap generation - those belong to later phases.

#### Phase 6 verification (completed)

Verified end-to-end against the real MySQL database with both services running:

| Check | Result |
| --- | --- |
| `mvn clean test` (backend) | **Tests run: 44, Failures: 0, Errors: 0, Skipped: 0** |
| `python -m pytest -q` (ai-engine) | **21 passed** |
| `GET http://localhost:8000/health` | 200 - `{"status":"UP","message":"Student Skill Gap Analyzer AI engine is running"}` |
| `GET http://localhost:8080/api/health` | 200 - `{"status":"UP","message":"Student Skill Gap Analyzer backend is running"}` |
| `GET http://localhost:8080/actuator/health` | 200 - `{"status":"UP"}` |
| Resume upload + Apache Tika extraction | real PDF stored with extracted text in `resumes` |
| Live skill extraction through the Python engine | HTTP 200 |

```http
POST /api/students/166/resumes/65/extract-skills

200 OK
{
  "studentId": 166,
  "resumeId": 65,
  "skills": [
    "GitHub", "Java", "Python", "HTML", "MySQL", "Power BI",
    "Spring Boot", "AWS", "Machine Learning", "REST APIs"
  ]
}
```

This proves the full flow: PDF → Spring Boot (`resumes.extracted_text`) → `AiEngineClient` → FastAPI
`POST /api/v1/skills/extract` → canonical skills → Spring Boot response.

Failure paths verified as well: unknown student, unknown resume and another student's resume → `404`;
resume without extracted text → `422`; Python engine stopped → `503` with a controlled JSON error.

### Phase 7A - Job description domain + required skills

**Phase 7A stores a target job description and the skills it requires. The matched / partially
matched / missing comparison is NOT implemented yet.**

```
StudentProfile 1 ──────── * JobDescription * ──────── * Skill
                                 (job_description_skills)
```

| Item | Value |
| --- | --- |
| Table | `job_descriptions`: `id`, `student_id` (FK → `student_profiles`), `title`, `company_name`, `description_text`, `created_at`, `updated_at` |
| Join table | `job_description_skills`: `job_description_id`, `skill_id`, unique on both columns (`uk_job_description_skills_job_skill`) |
| Required skills | canonical entries of the shared `skills` catalog - never duplicated as free text in the job-description table |
| AI reuse | the same Phase 6 `AiEngineClient` → `POST http://localhost:8000/api/v1/skills/extract`; no second HTTP client, no new Python endpoint, no Phase 5 extractor change |
| Flow | validate student → extract skills with the engine → resolve names case-insensitively (an unknown canonical name is registered once as `OTHER`) → save job description and join rows in one transaction |
| Failure behaviour | engine unreachable → `503`, engine error/invalid payload → `502`; extraction happens **before** anything is persisted, so a failed call leaves no job description behind |
| Ownership | every read and delete is scoped to the student in the URL - another student's job description returns `404` |
| Delete | removes the job description and its join rows; the shared `Skill` rows are kept |

Request / response example:

```http
POST /api/students/166/job-descriptions
{"title": "Java Backend Developer", "companyName": "Example Technologies",
 "descriptionText": "Looking for a Java developer with Spring Boot, REST APIs, MySQL, Git and AWS."}

201 Created
{"id": 12, "studentId": 166, "title": "Java Backend Developer", "companyName": "Example Technologies",
 "descriptionText": "Looking for a Java developer with ...",
 "requiredSkills": ["Java", "Spring Boot", "REST APIs", "MySQL", "Git", "AWS"],
 "createdAt": "2026-09-25T12:00:00Z", "updatedAt": "2026-09-25T12:00:00Z"}
```

Phase 7A limitations: no matched / partially matched / missing classification, no match percentage or
score, no roadmap or recommendations, no job-vs-job comparison, and extracted skills are not written
into `student_skills` automatically.

### Phase 7B - Skill-gap analysis (matched / partially matched / missing)

**Read-only analysis: no `MatchResult` entity and no `match_results` table exist yet. Every response is
calculated from persisted data on the fly - roadmaps and recommendations belong to Phase 7C.**

Endpoint: `GET /api/students/{studentId}/job-descriptions/{jobDescriptionId}/skill-gap` - the student in
the URL must own the job description, otherwise `404`.

Inputs: the student's persisted `student_skills` and the `requiredSkills` of that job description.
Nothing else is read, and nothing is written.

Matching algorithm (deterministic, no NLP, no fuzzy or semantic matching):

1. Normalize both names: lower case, trimmed, internal whitespace collapsed.
2. **MATCHED** - the normalized names are exactly equal (`Java` vs `java`).
3. **PARTIALLY_MATCHED** - the names differ, but one normalized name occurs inside the other as a whole
   token or a contiguous run of whole tokens: `Java` in `Java 17`, `Spring` in `Spring Boot`, `AWS` in
   `AWS Cloud`. Tokens are split on every non-alphanumeric character, so the rule stays boundary aware
   and refuses substring false positives: `SQL` does **not** partially match `MySQL`, and `Java` does
   **not** partially match `JavaScript`.
4. **MISSING** - neither of the above.

Score (transparent - no hidden weights):

```
matchPercentage = (matchedCount + 0.5 * partiallyMatchedCount) / totalRequiredSkills * 100
```

rounded half-up to two decimals, and `0.0` when the job description requires no skills. Classification
happens per required skill (the denominator), so duplicate required or duplicate student skills can
never inflate the result, and student skills the job does not ask for are ignored.

Example request and response:

```http
GET /api/students/208/job-descriptions/8/skill-gap

200 OK
{"studentId": 208, "jobDescriptionId": 8,
 "jobTitle": "Java Backend Developer", "companyName": "Phase7 Test Company",
 "totalRequiredSkills": 6,
 "matchedSkills": ["Java", "Spring Boot", "MySQL", "Git"],
 "partiallyMatchedSkills": [],
 "missingSkills": ["REST APIs", "AWS"],
 "matchedCount": 4, "partiallyMatchedCount": 0, "missingCount": 2,
 "matchPercentage": 66.67}
```

Edge cases covered by the integration tests: all required skills matched (`100.0`), none matched
(`0.0`), partial matches (half credit each), zero required skills (`0.0`), duplicate student skills,
duplicate required skills, extra student skills, unknown student (`404`), unknown job description
(`404`), another student's job description (`404`), `SQL` vs `MySQL`, `Java` vs `JavaScript`, and a
check that calling the endpoint leaves `student_skills` byte-for-byte unchanged.

### Phase 7C - Deterministic learning roadmap and recommendations

**Read-only roadmap: no persistence, no `MatchResult` entity or table, and the student's stored skills
are never modified. Everything is calculated from local data on the fly.**

Endpoint: `GET /api/students/{studentId}/job-descriptions/{jobDescriptionId}/roadmap` - the student in
the URL must own the job description, otherwise `404`.

Architecture: `SkillGapRoadmapService` consumes the output of `SkillGapAnalysisService` (Phase 7B)
without duplicating normalization, exact matching, token-aligned partial matching, or match scoring.
It adds deterministic priorities, transparent reasons, curated local learning topics, and step numbers.

Priority rules:
- **`HIGH`** - Missing required skills (skills required by the job but absent from the student's skills).
- **`MEDIUM`** - Partially matched skills (student has a related skill, but does not cover the exact requirement).
- **Omitted** - Fully matched skills are completely excluded from recommendations and the roadmap.

Ordering:
- Every `HIGH` priority step comes before every `MEDIUM` priority step.
- Within a priority tier, the order of skills preserves the job description's declaration order.
- Step numbering (`order`) starts at `1` and increments continuously.

Suggested learning topics:
- Sourced from a local in-memory catalog (`LearningTopicCatalog`) for known skills (Java, Spring Boot,
  REST APIs, MySQL, Git, AWS, Python, HTML, Power BI, Machine Learning, Docker, Kubernetes, React, etc.).
- Skills without a curated entry receive a reproducible fallback: `["fundamentals", "core concepts", "practical implementation", "common interview questions", "mini project"]`.
- No external AI API, LLM, or course scraping is used. Topics provide generic, structured learning
  guidance for technical concepts.

Example request and response:

```http
GET /api/students/208/job-descriptions/8/roadmap

200 OK
{
  "studentId": 208,
  "jobDescriptionId": 8,
  "jobTitle": "Java Backend Developer",
  "companyName": "Phase7 Test Company",
  "matchPercentage": 66.67,
  "currentSkills": ["Git", "Java", "MySQL", "Spring Boot"],
  "matchedSkills": ["Java", "Spring Boot", "MySQL", "Git"],
  "partiallyMatchedSkills": [],
  "missingSkills": ["REST APIs", "AWS"],
  "recommendations": [
    {
      "skill": "REST APIs",
      "priority": "HIGH",
      "reason": "Required by the target job but missing from the student's skills.",
      "suggestedTopics": ["HTTP methods", "REST principles", "JSON", "Status codes", "API design"]
    },
    {
      "skill": "AWS",
      "priority": "HIGH",
      "reason": "Required by the target job but missing from the student's skills.",
      "suggestedTopics": ["IAM", "EC2", "S3", "VPC", "Cloud basics"]
    }
  ],
  "roadmap": [
    {
      "order": 1,
      "skill": "REST APIs",
      "priority": "HIGH",
      "reason": "Required by the target job but missing from the student's skills.",
      "suggestedTopics": ["HTTP methods", "REST principles", "JSON", "Status codes", "API design"]
    },
    {
      "order": 2,
      "skill": "AWS",
      "priority": "HIGH",
      "reason": "Required by the target job but missing from the student's skills.",
      "suggestedTopics": ["IAM", "EC2", "S3", "VPC", "Cloud basics"]
    }
  ]
}
```

### Phase roadmap

| Phase | Scope | Status |
| --- | --- | --- |
| 1 | Repository structure + Spring Boot backend foundation (REST + Actuator) | **Done** |
| 2 | MySQL + Spring Data JPA persistence (StudentProfile entity, repository, MySQL integration test) | **Done** |
| 3 | Skill-management domain: Skill/SkillCategory/ProficiencyLevel, StudentSkill relationship, REST APIs, validation, error handling | **Done** |
| 4 | Resume upload + text extraction: Apache Tika (PDF/DOCX), file validation, `resumes` storage, resume APIs | **Done** |
| 5 | Python AI/NLP engine foundation: FastAPI service, canonical catalog, deterministic skill extraction/normalization | **Done** |
| 6 | Backend ↔ Python integration: `extract-skills` endpoint, `RestClient` AI client, controlled 502/503 errors | **Done - verified** |
| 7A | Job description domain: `job_descriptions` + `job_description_skills`, CRUD APIs, required-skill extraction through the AI engine | **Done - verified** |
| 7B | Skill-gap analysis: matched / partially matched / missing classification + transparent match percentage (read-only, no persistence) | **Done - verified** |
| 7C | Learning roadmap generation and recommendations (deterministic, read-only) | **Done - verification pending** |
| 8 | React frontend (upload, results, roadmap UI) | Planned |
| 9 | JWT authentication and user accounts | Planned |
| 10 | AWS deployment, CI/CD | Planned |
