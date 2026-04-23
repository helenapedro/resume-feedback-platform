# Codex Creator Challenge Submission

## Project Summary

Resume Feedback Platform is an enterprise-grade backend system that manages resumes, versioned uploads, secure sharing, comments, and AI-powered feedback. The project separates user-facing API latency from AI model latency by using an asynchronous Kafka-based worker pipeline, and it stores AI artifacts in MongoDB for reliable retrieval.

This submission highlights a backend-first solution that enables intelligent, version-aware resume review with a strong operational foundation.

## Why this project can win

- **Asynchronous AI feedback workflow**: AI feedback jobs are decoupled from resume upload and versioning, which keeps the user-facing API responsive even when the model takes time to complete.
- **Version-aware progress analysis**: The worker compares new resume versions to previous versions and prior feedback, producing a tailored progress summary rather than isolated reviews.
- **Secure sharing and audit**: Resume share links are token-based and revocable, with access and comment controls for external reviewers.
- **Strong architectural separation**: The repo uses a modular monorepo structure with `resume-api`, `resume-worker`, and `common` modules.
- **Persistence strategy for reliability**: MySQL stores transactional state and job lifecycle data, while MongoDB stores AI feedback documents and progress analysis payloads.
- **Backend language selection support**: AI feedback can be generated in English or Portuguese, with English as the default and Portuguese available explicitly.

## Key differentiators

- Kafka event-driven design instead of synchronous AI calls.
- AI progress analysis across resume versions, not just single-version feedback.
- Explicit retry and failure handling for AI jobs.
- Clear separation between core API behavior and AI processing responsibilities.
- Focus on backend robustness and real-world deployability.

## Architecture and flow

1. User uploads a resume or creates a new version via the REST API.
2. The API persists resume metadata in MySQL and creates an `AiJob` record with status `PENDING`.
3. A worker consumes the AI job, extracts resume text, generates Gemini feedback, and persists the result in MongoDB.
4. The worker optionally generates progress analysis when a previous version and baseline feedback exist.
5. The user can poll job status and retrieve AI feedback or progress results through the API.

## Demo workflow

1. Start local infrastructure:

```bash
cd "c:/Users/mbeua/Área de Trabalho/resume-feedback-platform"
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.kafka.yml up -d
```

2. Run the API and worker:

```bash
./mvnw -pl resume-api spring-boot:run
./mvnw -pl resume-worker spring-boot:run
```

3. Create a resume and upload a version, then check AI status:

```bash
POST /api/auth/login
POST /api/resumes
POST /api/resumes/{resumeId}/versions
GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest
GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback
GET /api/resumes/{resumeId}/versions/{versionId}/ai-progress
```

4. Regenerate feedback in Portuguese:

```bash
POST /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/regenerate?language=PT
```

## Evaluation strengths

- **Usability**: The API is designed for client apps and external reviewers, with secure token sharing and versioned resume access.
- **Reliability**: The system persists job state, supports retrying failed jobs, and avoids blocking API requests on AI latency.
- **Innovation**: Combining AI resume feedback with version-aware progress analysis is a strong candidate for a challenge entry.
- **Extensibility**: The architecture can be extended to support additional AI providers, more languages, and richer evaluation metrics.

## Submission checklist

- [ ] Root `README.md` includes challenge summary and demo guidance.
- [ ] `docs/submission.md` describes submission value and key strengths.
- [ ] Build passes for `common`, `resume-api`, and `resume-worker`.
- [ ] Key API flows are documented clearly.
- [ ] No secrets are committed.
- [ ] Final branch is clean and ready for submission.
