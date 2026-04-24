# Resume Feedback Platform

Resume Feedback Platform is a live product for uploading resumes, generating AI feedback, tracking improvements across versions, and sharing review links with controlled access.

This repo is the backend monorepo behind the hosted app at `https://feedback.hmpedro.com`. The frontend is a separate React + TypeScript application hosted on AWS Amplify, while this repository contains the Spring Boot API and worker services deployed on Heroku. It is also the project package for the Codex Creator Challenge.

## What judges should see first

- Live app: `https://feedback.hmpedro.com`
- Hosted frontend flow: landing page, auth, resume library, profile, version history, AI feedback, comments, and share links
- Judge guide: [JUDGES_GUIDE.md](JUDGES_GUIDE.md)
- Demo guide: [DEMO.md](DEMO.md)
- Codex build story: [docs/codex-build-story.md](docs/codex-build-story.md)
- Submission summary: [docs/submission.md](docs/submission.md)

## Why this entry is strong

- Real product, not a toy endpoint set: there is a deployed UI and a working end-to-end workflow.
- AI feedback is asynchronous: uploads stay responsive while a background worker processes Gemini jobs.
- Version-aware analysis: the platform compares resume versions instead of treating each upload in isolation.
- Shareable review workflow: token-based links, comments, and access auditing support recruiter or mentor review.
- Practical engineering choices: MySQL for transactional state, MongoDB for AI artifacts, optional Kafka support for event-driven processing, and production-friendly cost controls such as worker polling and disabled Redis-backed rate limiting.

## Core product flow

1. Sign in and upload a resume PDF.
2. The API stores metadata and creates an AI job.
3. The worker processes pending AI jobs and generates feedback plus progress analysis.
4. The UI shows version history, AI feedback, comments, and share-link controls.
5. Users iterate on new resume versions and compare improvement over time.

## Repository layout

- `resume-api`: REST API for auth, resumes, share links, comments, AI job orchestration, and retrieval
- `resume-worker`: background worker for AI feedback generation and progress analysis
- `common`: shared message contracts and cross-module models

## Quick evaluator path

If you want the fastest way to understand the project:

1. Open the live app at `https://feedback.hmpedro.com`
2. Read [JUDGES_GUIDE.md](JUDGES_GUIDE.md)
3. Skim [docs/submission.md](docs/submission.md)
4. Use [DEMO.md](DEMO.md) only if you want to run the stack locally

## Local development

Prerequisites:

- Java 17
- Maven Wrapper
- Docker Desktop

Start the local stack:

```bash
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.kafka.yml up -d
./mvnw -pl resume-api spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw -pl resume-worker spring-boot:run -Dspring-boot.run.profiles=dev
```

For the full local walkthrough, see [DEMO.md](DEMO.md). For runtime details, see [docs/operations.md](docs/operations.md).

## Focused execution coverage

This repo now includes higher-signal automated coverage around the parts judges will care about most:

- AI job creation and async handoff behavior are covered
- Share-link validation records both successful and failed access audits
- Worker job processing covers success and retry/failure paths

## Documentation

- [JUDGES_GUIDE.md](JUDGES_GUIDE.md)
- [DEMO.md](DEMO.md)
- [FINAL_SUBMISSION.md](FINAL_SUBMISSION.md)
- [Documentation Index](docs/README.md)
- [Architecture](docs/architecture.md)
- [Requirements](docs/requirements.md)
- [Operations](docs/operations.md)
- [Submission](docs/submission.md)
- [Codex Build Story](docs/codex-build-story.md)

## Next improvements

- Add a short public demo video or GIF-based walkthrough
- Add richer before-vs-after resume examples in the submission packet
- Add one thin judge account or scripted seeded demo data path
- Add a single end-to-end browser smoke test for the hosted frontend
