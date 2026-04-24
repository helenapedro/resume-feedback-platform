# Demo Guide

This is the shortest reliable way to run the project locally and validate the core workflow.

## Supported local path

Use the `dev` profile. It is the maintained local setup for this repo.

## Prerequisites

- Java 17
- Docker Desktop
- Maven Wrapper (`mvnw` or `mvnw.cmd`)

## 1. Start infrastructure

```bash
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.kafka.yml up -d
```

Services used locally:

- MySQL on `localhost:3306`
- MongoDB on `localhost:27017`
- Redis on `localhost:6379`
- Kafka on `localhost:9092`

## 2. Run the API

```bash
./mvnw -pl resume-api spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected result:

- API starts on `http://localhost:8080`
- startup no longer depends on `APP_JWT_SECRET`
- resume files are stored under `resume-api/var/resumes`

## 3. Run the worker

```bash
./mvnw -pl resume-worker spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected result:

- worker connects to MySQL, MongoDB, and Kafka
- AI jobs move from `PENDING` to `DONE` when processed

## 4. Five-minute evaluator workflow

1. Register or log in
2. Upload a resume PDF
3. Open the resume detail page
4. Wait for AI feedback to finish
5. Review:
   - summary
   - strengths
   - suggested improvements
   - version history
   - comments
   - share links

## 5. API checkpoints

After login, these endpoints validate the core execution path:

- `POST /api/resumes`
- `GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest`
- `GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback`
- `POST /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/regenerate?language=PT`
- `GET /api/resumes/{resumeId}/versions/{versionId}/ai-progress`

## 6. What this demo proves

- The upload flow is real
- The async AI pipeline is real
- Kafka-backed job orchestration is wired
- AI feedback retrieval works
- Version-aware progress analysis works
- The product supports sharing and collaboration workflows

## 7. If something fails

- API fails on startup: verify the `dev` profile is being used
- Worker fails on startup: verify the datasource URL starts with `jdbc:mysql://`
- AI jobs stay `PENDING`: verify Kafka and the worker are both running
- No real model output: verify the Gemini key is configured for the worker

For deeper operational notes, use [docs/operations.md](docs/operations.md).
