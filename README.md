# Resume Feedback Platform

**Resume Feedback Platform is featured in the [Handshake AI Showcase](https://app.joinhandshake.com/ai-showcase?project_id=3056375).**

![Live Demo Image](/docs/project-images/live-demo-home-page.png)

Resume Feedback Platform helps candidates turn resume feedback from a one-time, subjective review into a repeatable improvement loop. Users can upload a resume in Portuguese or English, get structured language-aware AI feedback, track progress across versions, and share controlled review links with mentors, recruiters, or peers.

This repo is the backend monorepo for Resume Feedback Platform. The frontend is a separate React + TypeScript application hosted on AWS Amplify, while this repository contains the Spring Boot API and worker services deployed on Heroku. Development was assisted by GitHub Copilot inside VS Code.

## Problem

The idea for Resume Feedback Platform came from conversations with talented, qualified people who asked for help planning their careers. When I looked at their resumes, I saw a painful gap: the documents did not communicate the real professional value those people already had.

Many companies are genuinely looking for candidates like them. The candidate may have experience, technical ability, potential, and a strong desire to grow. But a recruiter cannot guess that from context they do not have. The resume is often the first entry point, and writing a strong resume is its own skill.

Over time, I saw smart, capable people miss opportunities not because they lacked talent, but because their resumes did not present their professional journey clearly, structurally, and strategically. Resume Feedback Platform was created to reduce that communication barrier and help people make their talent, knowledge, and potential more visible through a continuous resume improvement process.

## Impact

Resume Feedback Platform creates a single workflow for resume iteration:

- Candidates get structured AI feedback tied to a specific resume version.
- New uploads can be compared against previous versions, making progress visible instead of anecdotal.
- Share links let outside reviewers view or comment without exposing the whole account.
- Access controls, audit records, and token revocation make sharing safer than sending resume files manually.

## Recognition

**Resume Feedback Platform is featured in the [Handshake AI Showcase](https://app.joinhandshake.com/ai-showcase?project_id=3056375).**

![Handshake Featured Image](https://resume-feedback-platform.s3.us-east-1.amazonaws.com/images/handshake_feature.png)

## Start here

- Live app: `https://feedback.hmpedro.com`
- Handshake AI Showcase: `https://app.joinhandshake.com/ai-showcase?project_id=3056375`
- Frontend repo: `https://github.com/helenapedro/ui_ai-powered-resume-feedback-platform`
- Agents League submission notes: [docs/agents-league-submission.md](docs/agents-league-submission.md)
- API reference: [docs/project-documentation/API.md](docs/project-documentation/API.md)
- Architecture: [docs/project-documentation/architecture.md](docs/project-documentation/architecture.md)
- Operations: [docs/project-documentation/operations.md](docs/project-documentation/operations.md)

## What We Built

- **Real product**: there is a deployed UI and a working end-to-end workflow.
- **AI feedback is asynchronous**: uploads stay responsive while a background worker processes LLM jobs.
- **Version-aware analysis**: the platform compares resume versions.
- **Portuguese and English resume support**: the product supports users who submit resumes in Portuguese or English and need feedback that fits their language context.
- **Shareable review workflow**: token-based links, comments, and access auditing support recruiter or mentor review.
- **Practical engineering choices**: **_MySQL_** for transactional state, **_MongoDB_** for AI artifacts, **_Kafka_** for event-driven processing, and **_Redis-backed rate limiting_**.
- **GitHub Copilot** assisted development in VS Code, especially for debugging, documentation, and reasoning through safe implementation boundaries.
- The **worker** uses an `AiProviderClient` abstraction. `APP_AI_PROVIDER` lets each environment select the provider that best fits its needs, including Azure OpenAI for Microsoft Foundry-compatible deployments.

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
3. Skim [docs/project-documentation/architecture.md](docs/project-documentation/architecture.md)
4. Review [docs/agents-league-submission.md](docs/agents-league-submission.md) for the hackathon-focused story and demo flow

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

For runtime details, see [docs/project-documentation/operations.md](docs/project-documentation/operations.md).

## Focused execution coverage

This repo includes focused automated coverage around the highest-risk execution paths:

- AI job creation and async handoff behavior are covered
- Share-link validation records both successful and failed access audits
- Worker job processing covers success and retry/failure paths

## Documentation

- [Documentation Index](docs/README.md)
- [API Reference](docs/project-documentation/API.md)
- [Architecture](docs/project-documentation/architecture.md)
- [Requirements](docs/project-documentation/requirements.md)
- [Operations](docs/project-documentation/operations.md)
- [Agents League Submission](docs/agents-league-submission.md)

## Next improvements

- Add a short public demo video or GIF-based walkthrough
- Add richer before-vs-after resume examples in the product documentation
- Add one scripted seeded demo data path
- Add a single end-to-end browser smoke test for the hosted frontend
