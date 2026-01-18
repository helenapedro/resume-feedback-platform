# Resume Feedback Platform

## 🚀 Features

- Resume upload with versioning (S3)
- Secure sharing via expiring links
- AI-generated feedback (async)
- Commenting system per version
- Audit logging and access tracking

## 🏗 Architecture

- API: Spring Boot + JPA/Hibernate
- AI Processing: Async jobs via message queue
- Storage:
  - PostgreSQL (core data)
  - MongoDB (AI feedback)
  - AWS S3 (resume files)

## 📦 Tech Stack

- Java 21, Spring Boot
- PostgreSQL, MongoDB
- AWS S3, SQS
- JWT Authentication

## ▶️ Running Locally

```bash
docker compose up -d
./mvnw spring-boot:run
```

## 📚 Documentation

- System Requirements: [docs/system-requirements.md](docs/system-requirements.md)
- Architecture Diagrams: [docs/architecture](docs/architecture.md)
