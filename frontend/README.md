# frontend

React application for the **Student Skill Gap Analyzer**.

## Status

Placeholder - not implemented in Phase 1.

## Planned content (later phases)

- React app (created with Vite/CRA in a later phase) that talks to the Spring Boot backend over REST.
- **Resume upload** screen - drag & drop PDF/DOCX resume plus a job description text area.
- **Analysis results** screen - matched / partially matched / missing skills with match percentages.
- **Learning roadmap** screen - prioritized skills to learn, suggested resources, and progress tracking.
- **Auth screens** - login and registration once JWT authentication is added.
- Shared API client layer that centralizes calls to the backend REST endpoints.

## Related folders

- `../backend` - Spring Boot REST API consumed by this app.
- `../ai-engine` - Python service that performs the actual skill extraction and gap analysis.
