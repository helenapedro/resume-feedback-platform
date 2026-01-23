# resume-api

REST API for resume upload/versioning, secure sharing, and audit logging.

## Overview
- Handles authentication, resume metadata, versioning, and share links.
- Stores resume metadata in MySQL and files in local storage (dev).
- Provides secure, token-based public access for share links.
- Publishes AI job events to Kafka and exposes AI feedback read endpoints backed by MongoDB.

## Tech Stack
- Java 17 + Spring Boot
- Spring Security (JWT)
- Flyway (schema migrations)
- MySQL (metadata, security, audit)
- MongoDB (AI feedback documents)
- Kafka (AI job events)
- Local filesystem storage (dev)

## Local Development
1) Start MySQL (from the `feedback` root):

```bash
cd docker
docker compose up -d
```

2) Start Kafka and MongoDB (see local guides):
- Kafka: `docs/kafka-local.md`
- MongoDB: `docs/mongodb-local.md`

3) Run the API (from `feedback/resume-api`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Authentication
### Register
- `POST /api/auth/register`

```json
{"email":"pedro@local.dev","password":"123456"}
```

### Login
- `POST /api/auth/login`

```json
{"email":"pedro@local.dev","password":"123456"}
```

## Resume Endpoints (JWT required)
- `GET /api/resumes`
- `POST /api/resumes` (multipart)
- `POST /api/resumes/{id}/versions` (multipart)
- `GET /api/resumes/{id}`
- `GET /api/resumes/{resumeId}/versions/{versionId}/download`

## Share Links
### Owner (JWT)
- `POST /api/resumes/{resumeId}/share-links`
- `GET /api/resumes/{resumeId}/share-links`
- `POST /api/resumes/{resumeId}/share-links/{linkId}/revoke`

### Public (token)
- `GET /api/share/{token}` (metadata)
- `GET /api/share/{token}/download` (download current version)

Example:
```bash
curl -L -o resume.pdf http://localhost:8080/api/share/<TOKEN>/download
```

## Comments
### Owner (JWT)
- `GET /api/resumes/{resumeId}/versions/{versionId}/comments`
- `POST /api/resumes/{resumeId}/versions/{versionId}/comments`

### Public (token with COMMENT permission)
- `GET /api/share/{token}/comments`
- `POST /api/share/{token}/comments`

## AI Feedback (JWT)
- `GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback` (latest feedback)

## AI Jobs (JWT)
- `GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest`
- `POST /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/regenerate`

## Database Schema (Tables)
- `access_audit`
- `ai_feedback_refs`
- `ai_jobs`
- `comments`
- `resume_versions`
- `resumes`
- `share_links`
- `users`

## Notes
- Share tokens are stored only as SHA-256 hashes.
- Share links return the plaintext token only once at creation time.
