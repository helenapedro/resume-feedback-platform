# Resume Feedback Platform

Resume Feedback Platform helps candidates turn resume feedback from a one-time, subjective review into a repeatable improvement loop. Users can upload a resume, get structured AI feedback, track progress across versions, and share controlled review links with mentors, recruiters, or peers.

This repo is the backend monorepo behind the hosted app at `https://feedback.hmpedro.com`. The frontend is a separate React + TypeScript application hosted on AWS Amplify, while this repository contains the Spring Boot API and worker services deployed on Heroku.

## Problem

Resume improvement is usually fragmented. Candidates receive advice from different people, paste resumes into generic AI tools, lose track of what changed between versions, and have no clean way to invite reviewers without sending files around manually.

That creates three practical problems:

- Feedback is hard to compare across resume versions.
- Review collaboration happens outside the resume workflow.
- Candidates do not get a clear record of whether each revision actually improved the resume.

## Impact

Resume Feedback Platform creates a single workflow for resume iteration:

- Candidates get structured AI feedback tied to a specific resume version.
- New uploads can be compared against previous versions, making progress visible instead of anecdotal.
- Share links let outside reviewers view or comment without exposing the whole account.
- Access controls, audit records, and token revocation make sharing safer than sending resume files manually.
- The hosted product is live and discoverable through the Handshake AI Showcase.

## Recognition

**Resume Feedback Platform is featured in the [Handshake AI Showcase](https://app.joinhandshake.com/ai-showcase?project_id=3056375).**

![Handshake Featured Image](https://resume-feedback-platform.s3.us-east-1.amazonaws.com/images/handshake_feature.png)

## Start here

- Live app: `https://feedback.hmpedro.com`
- Handshake AI Showcase: `https://app.joinhandshake.com/ai-showcase?project_id=3056375`
- Frontend repo: `https://github.com/helenapedro/ui_ai-powered-resume-feedback-platform`
- Demo guide: [DEMO.md](DEMO.md)
- API reference: [docs/API.md](docs/API.md)
- Architecture: [docs/architecture.md](docs/architecture.md)
- Operations: [docs/operations.md](docs/operations.md)

## What We Built

- Real product, not a toy endpoint set: there is a deployed UI and a working end-to-end workflow.
- AI feedback is asynchronous: uploads stay responsive while a background worker processes LLM jobs.
- Version-aware analysis: the platform compares resume versions instead of treating each upload in isolation.
- Shareable review workflow: token-based links, comments, and access auditing support recruiter or mentor review.
- Practical engineering choices: MySQL for transactional state, MongoDB for AI artifacts, Kafka for event-driven processing, and Redis-backed rate limiting.

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

## Quick product path

If you want the fastest way to understand the project:

1. Open the live app at `https://feedback.hmpedro.com`
2. Review the product workflow in the hosted frontend
3. Skim [docs/architecture.md](docs/architecture.md)
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

This repo includes focused automated coverage around the highest-risk execution paths:

- AI job creation and async handoff behavior are covered
- Share-link validation records both successful and failed access audits
- Worker job processing covers success and retry/failure paths

## Documentation

- [DEMO.md](DEMO.md)
- [Documentation Index](docs/README.md)
- [API Reference](docs/API.md)
- [Architecture](docs/architecture.md)
- [Requirements](docs/requirements.md)
- [Operations](docs/operations.md)
- [Codex Build Story](docs/codex-build-story.md)

## Next improvements

- Add a short public demo video or GIF-based walkthrough
- Add richer before-vs-after resume examples in the product documentation
- Add one scripted seeded demo data path
- Add a single end-to-end browser smoke test for the hosted frontend
