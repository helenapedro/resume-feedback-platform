# Codex Creator Challenge Submission

## One-line pitch

Resume Feedback Platform helps candidates improve their resumes with AI-generated feedback, version-to-version progress tracking, and controlled sharing for mentors or recruiters.

## Why this project stands out

- It is already deployed and usable, not just a local prototype.
- It combines product UX with backend depth: auth, versioning, async AI jobs, sharing, comments, and auditing.
- It treats resumes as evolving documents, so users can see improvement over time.
- It uses an asynchronous worker architecture that is appropriate for slow AI workloads.

## What the user can do

1. Sign in
2. Upload a resume PDF
3. Wait for AI feedback to process
4. Review strengths and suggested improvements
5. Upload a new version and compare progress
6. Share the resume for comments with controlled access

## Real deployment topology

- Frontend: separate React + TypeScript app hosted on AWS Amplify
- Backend API: Spring Boot on Heroku web dyno
- Background processing: Spring Boot worker on Heroku worker dyno
- Relational data: MySQL
- AI documents: MongoDB

The codebase supports Kafka and Redis integrations, especially for local development and future scaling, but the current hosted deployment intentionally avoids paid Heroku Kafka and Redis add-ons to keep operating costs low. In production, the worker processes pending jobs through the scheduler and the API runs with share-link rate limiting disabled.

## Technical implementation

- `resume-api` owns the user-facing workflow and persistence for transactional state
- `resume-worker` processes AI jobs and generates AI artifacts
- `common` shares cross-service contracts
- MySQL stores resumes, users, links, comments, and AI job state
- MongoDB stores AI feedback and progress documents
- Optional Kafka support exists in the codebase, but the live deployment currently relies on the polling worker path

## Why Codex mattered

Codex was useful as an engineering accelerator, not just a code generator:

- debugging multi-module build and startup issues
- hardening local development profiles
- tightening async pipeline behavior around commit timing
- adding focused automated tests where they matter most
- packaging the repo into a challenge-ready submission

See [docs/codex-build-story.md](codex-build-story.md) for the fuller build story.

## Focused execution evidence

This repo now emphasizes the paths that most clearly prove execution quality:

- AI job creation and async handoff behavior are covered
- share-link validation is covered with access-audit assertions
- worker processing covers both successful completion and retry behavior

These are higher-signal for a challenge evaluator than broad but shallow test volume.

## Recommended evaluator path

1. Visit `https://feedback.hmpedro.com`
2. Review [JUDGES_GUIDE.md](../JUDGES_GUIDE.md)
3. Skim the architecture and operations docs only if deeper implementation detail is needed

## What to improve next

- Add one short product demo video
- Add seeded sample resumes with before-and-after AI outputs
- Add one browser-level smoke test for the hosted UI
