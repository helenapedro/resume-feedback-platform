# resume-api

Synchronous REST API module for authentication, resumes, share links, comments, and AI job orchestration.

For platform context and architecture, see the [root README](../README.md).

## Purpose

`resume-api` is the entry point for client applications. It handles user-facing operations and publishes AI processing requests to Kafka.

## Responsibilities

- User registration and login (JWT)
- Resume CRUD and versioning
- Secure share-link lifecycle (create/list/revoke)
- Owner/public comments
- AI job creation/status/regeneration endpoints
- AI feedback read endpoints
- Audit and access logging

## Key Dependencies

- Spring Boot Web MVC
- Spring Security (JWT)
- Spring Data JPA (MySQL)
- Spring Data MongoDB
- Spring Data Redis
- Spring Kafka
- Flyway
- AWS SDK S3

## Local Run

From repo root:

```bash
./mvnw -pl resume-api spring-boot:run
```

Common profile usage:

```bash
./mvnw -pl resume-api spring-boot:run -Dspring-boot.run.profiles=dev
```

## Configuration (Important)

Core dependencies are provided via environment variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_MONGODB_URI`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_PREFIX` (optional topic/group prefix)

Storage:

- `app.storage.backend` (`LOCAL` or `S3`)
- local path: `app.storage.local-dir`

## API Surface (High-Level)

Auth:
- `POST /api/auth/register`
- `POST /api/auth/login`

Resumes:
- `GET /api/resumes`
- `POST /api/resumes` (multipart)
- `POST /api/resumes/{id}/versions` (multipart)
- `GET /api/resumes/{id}`
- `GET /api/resumes/{resumeId}/versions/{versionId}/download`

Share Links:
- `POST /api/resumes/{resumeId}/share-links`
- `GET /api/resumes/{resumeId}/share-links`
- `POST /api/resumes/{resumeId}/share-links/{linkId}/revoke`
- `GET /api/share/{token}`
- `GET /api/share/{token}/download`

Comments:
- `GET /api/resumes/{resumeId}/versions/{versionId}/comments`
- `POST /api/resumes/{resumeId}/versions/{versionId}/comments`
- `GET /api/share/{token}/comments`
- `POST /api/share/{token}/comments`

AI:
- `GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback`
- `GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest`
- `POST /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/regenerate`

## Tests

```bash
./mvnw -pl resume-api test
```

## Notes

- Share tokens are persisted as hashes (plaintext returned only at creation time).
- AI events are published after transaction commit to avoid race conditions with worker consumption.
