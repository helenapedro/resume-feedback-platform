# Resume Feedback Platform
This repository follows a modular backend-first architecture, designed for scalability, security, and asynchronous processing.

## 🚀 Features
#### Core (Implemented)
- Secure user registration and authentication (JWT)
- Resume upload with immutable versioning
- Owner-based access control for all resume operations
- Secure resume download (owner & public)
- Shareable resume links with:
  - Unguessable tokens (SHA-256 hashed)
  - Expiration support
  - Revocation
  - Usage limits
- Full audit logging for shared access and downloads
- Centralized error handling with consistent API responses
- Commenting system per resume version (owner + external reviewers)

#### In Progress / Planned
- AI-generated resume feedback (async processing)
- AI job retry and status tracking
- Presigned URL support for S3 downloads
- Rate limiting for public share endpoints

## 🏗 Architecture Overview

The system is intentionally split into independent modules to allow isolated scaling and clear responsibility boundaries.

```powershell
resume-feedback-platform
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
- Java 21
- Spring Boot
- Spring Security (JWT)
- JPA / Hibernate
#### Datastores
- MySQL – source of truth for users, resumes, versions, sharing, audits
- MongoDB – AI feedback documents (versioned, semi-structured)
- AWS S3 – resume file storage (local filesystem in dev)
#### Infrastructure
- Docker / Docker Compose
- Message queue (planned: AWS SQS or equivalent)
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
#### 1. Start infrastructure
```bash
cd docker
docker compose up -d
```
#### 2. Run API
```bash
cd resume-api
./mvnw spring-boot:run
```
<The API will start using local storage for files and MySQL for metadata.>

## 🔐 Authentication
- JWT-based authentication
- Stateless API
- Owner identity resolved via SecurityContext
- Public endpoints authorized exclusively via secure tokens

## 📚 Documentation
- System Requirements: [docs/system-requirements.md](docs/system-requirements.md)
- Architecture Notes: [docs/architecture](docs/architecture.md)
- API Reference (resume-api): [feedback/resume-api](feedback/resume-api/README.md)

## 🧭 Project Status
This project is being developed incrementally with a backend-first approach.

The resume-api module is currently the primary focus, with an emphasis on:

- correctness
- security
- observability
- production-ready patterns

Frontend integration and AI processing are intentionally deferred until the backend contract is stable.
