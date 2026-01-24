# Resume Feedback Platform
This repository follows a modular backend-first architecture, designed for scalability, security, and asynchronous processing.

## 🚀 Features
#### Core (Implemented)
- Secure user registration and authentication (JWT)
- Resume upload with immutable versioning
- Owner-based access control for all resume operations
- Secure resume download (owner & public)
- Presigned URL support for S3 downloads (production profile)
- Shareable resume links with:
  - Unguessable tokens (SHA-256 hashed)
  - Expiration support
  - Revocation
  - Usage limits
- Full audit logging for shared access and downloads
- Centralized error handling with consistent API responses
- Redis-backed rate limiting for public share endpoints
- Commenting system per resume version (owner + external reviewers)
- AI-generated resume feedback (async processing via Kafka + worker)
- AI job retry, status tracking, and error metadata

#### In Progress / Planned
- Frontend integration

## 🏗 Architecture Overview

The system is intentionally split into independent modules to allow isolated scaling and clear responsibility boundaries.

```powershell
resume-feedback-platform
└── feedback
    ├── common            # Shared utilities, error model, base abstractions
    ├── resume-api        # Core REST API (auth, resumes, sharing, comments)
    └── resume-worker     # Async AI processing (queue consumers)
```

## Design Principles
- Layered architecture (Controller → Service → Repository)
- Strong ownership enforcement at service layer
- No distributed transactions
- Eventual consistency for AI workflows
- Forward-compatible storage abstractions

## 📦 Tech Stack
#### Backend
- Java 17
- Spring Boot
- Spring Security (JWT)
- JPA / Hibernate
#### Datastores
- MySQL – source of truth for users, resumes, versions, sharing, audits
- MongoDB – AI feedback documents (versioned, semi-structured)
- AWS S3 – resume file storage (local filesystem in dev)
- Redis – shared rate limiting for public share endpoints
#### Infrastructure
- Docker / Docker Compose
- Kafka for async AI job events
- JWT-based stateless authentication

## 📊 Data Model Highlights
- Resumes are logical documents owned by a user
- Resume Versions are immutable and sequential
- Share Links:
  - Store only hashed tokens
  - Support permissions (VIEW / COMMENT)
  - Are auditable and revocable
- Audit Logs track every sensitive public access
- AI Jobs are tracked independently from user requests

## ▶️ Running Locally (Development)
#### 1. Start infrastructure (from `feedback`)
```bash
cd feedback/docker
docker compose up -d
```
#### 2. Start Kafka + MongoDB (local guides)
- Kafka: [docs/kafka-local.md](docs/kafka-local.md)
- MongoDB: [docs/mongodb-local.md](docs/mongodb-local.md)
- Redis: [docs/redis-local.md](docs/redis-local.md)
#### 3. Run API
```bash
cd feedback/resume-api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
<The API will start using local storage for files and MySQL for metadata.>
#### 4. Run Worker
```bash
cd feedback/resume-worker
./mvnw spring-boot:run
```

## 🔐 Authentication
- JWT-based authentication
- Stateless API
- Owner identity resolved via SecurityContext
- Public endpoints authorized exclusively via secure tokens

## 📚 Documentation
- System Requirements: [docs/system-requirements.md](docs/system-requirements.md)
- Architecture Notes: [docs/architecture](docs/architecture.md)
- Kafka Local Setup: [docs/kafka-local.md](docs/kafka-local.md)
- MongoDB Local Setup: [docs/mongodb-local.md](docs/mongodb-local.md)
- Module README (common): [feedback/common/README.md](feedback/common/README.md)
- Module README (resume-api): [feedback/resume-api/README.md](feedback/resume-api/README.md)
- Module README (resume-worker): [feedback/resume-worker/README.md](feedback/resume-worker/README.md)

## 🧭 Project Status
This project is being developed incrementally with a backend-first approach.

The resume-api module is currently the primary focus, with an emphasis on:

- correctness
- security
- observability
- production-ready patterns

Frontend integration and AI processing are intentionally deferred until the backend contract is stable.

## 👤 Maintainer
- Helena (LinkedIn): https://www.linkedin.com/in/helena-software-engineer
