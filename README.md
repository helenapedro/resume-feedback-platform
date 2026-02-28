# Resume Feedback Platform

Enterprise-grade platform for resume management, secure sharing, versioned feedback, and AI-assisted analysis.

This repository is a multi-module Spring Boot monorepo with:
- `resume-api`: synchronous REST API for auth, resumes, share links, comments, and AI job orchestration.
- `resume-worker`: asynchronous worker that consumes Kafka jobs and generates AI feedback.
- `common`: shared contracts and cross-module models (including Kafka payloads).

## Quick Start

### 1) Prerequisites
- Java 17
- Maven Wrapper (`mvnw` / `mvnw.cmd`)
- MySQL
- MongoDB
- Kafka
- Redis (for rate-limit features)

### 2) Build all modules
```bash
./mvnw -DskipTests package
```

### 3) Run API
```bash
./mvnw -pl resume-api spring-boot:run
```

### 4) Run worker
```bash
./mvnw -pl resume-worker spring-boot:run
```

### 5) Validate
- Login: `POST /api/auth/login`
- Upload resume: `POST /api/resumes` (multipart: `file`, `title`)
- Check latest AI job: `GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest`

## Documentation

- [Documentation Index](docs/README.md)
- [Architecture](docs/architecture.md)
- [Requirements](docs/requirements.md)
- [Operations](docs/operations.md)

---

## Business Context and Scope

### Executive Summary
The platform allows users to upload and version resumes, share them through controlled links, receive comments, and generate AI feedback asynchronously.  
It separates user-facing API latency from AI processing latency by moving feedback generation to a Kafka-based worker pipeline.

### Problem It Solves
- Resume review is usually manual and inconsistent.
- Sharing and auditing resume access is often insecure or not traceable.
- AI processing can be slow and should not block upload flows.

### In Scope
- User registration/login (JWT)
- Resume upload, listing, download, and versioning
- Secure share links with audit trail
- Owner/public comments
- Asynchronous AI job creation and status tracking
- AI feedback persistence and retrieval

### Out of Scope (Current)
- Multi-tenant organization model
- Real-time WebSocket notifications for job completion
- Advanced prompt management UI
- Full SLO dashboards as code

### Target Users
- Resume owners (candidates/professionals)
- Recruiters/managers reviewing shared resumes
- Platform operators/developers

---

## Requirements

Detailed functional and non-functional requirements are available at:
- [docs/requirements.md](docs/requirements.md)

---

## Architecture

Detailed architecture documentation is available at:
- [docs/architecture.md](docs/architecture.md)

---

## Technical Decisions and Trade-offs

### Key Decisions
- Async AI processing via Kafka instead of synchronous API calls.
- Dual persistence: MySQL (core state) + MongoDB (feedback document model).
- JWT-based stateless auth for API endpoints.

### Trade-offs
- Pros: responsiveness, decoupling, better throughput under AI latency.
- Cons: eventual consistency and operational complexity (Kafka + worker).

### Known Limitations
- AI completion is not immediate; clients must poll.
- Health checks can reflect downstream dependency latency.

### Technical Risks
- Provider/API key misconfiguration may stall or fail jobs.
- Broker/auth settings can break event flow if environment drift occurs.

---

## Challenges and Lessons Learned

- Kafka SSL and deserializer compatibility required explicit environment handling.
- AI payload serialization required explicit Java Time support for `Instant`.
- Event publication timing needed post-commit publishing to avoid race conditions where worker reads before DB commit.

---

## Operations

Detailed runtime, environment, testing, deployment, and troubleshooting guidance is available at:
- [docs/operations.md](docs/operations.md)

---

## Roadmap

- Add push notifications/webhooks for AI job completion.
- Add richer observability (metrics dashboards and tracing).
- Improve retry/dead-letter strategy for poison events.
- Add admin UI for operational visibility and reprocessing.
- Expand AI provider abstraction and prompt version governance.

---

## Module READMEs
- [API Module](resume-api/README.md)
- [Worker Module](resume-worker/README.md)
- [Common Module](common/README.md)
