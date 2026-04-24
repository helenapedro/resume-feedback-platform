# Judges Guide

This project is easiest to evaluate as a product first and a codebase second.

## Fastest path

1. Open `https://feedback.hmpedro.com`
2. Review the landing page and sign-in flow
3. Open a resume detail screen and inspect:
   - PDF preview
   - AI feedback
   - comments
   - version history
   - share links
4. Skim [docs/submission.md](docs/submission.md) for architecture and challenge fit

## What makes this more than a CRUD app

- Resume versions are treated as a timeline, not flat files.
- AI work is queued and processed asynchronously so uploads do not block on model latency.
- Feedback is stored and retrieved separately from transactional state.
- Share links are revocable, auditable, and designed for real reviewer workflows.
- The frontend is a separate React + TypeScript app hosted on AWS Amplify; this repo contains the backend services deployed on Heroku.

## Core capabilities to look for

- Authentication and profile management
- Resume upload and version history
- AI feedback generation
- Version-to-version progress analysis
- Comment collaboration
- Share-link access control

## Architecture in one paragraph

`resume-api` handles user-facing REST operations and creates `AiJob` records. `resume-worker` processes those jobs in the background, extracts resume text, calls Gemini, writes AI feedback and progress analysis, and updates job state. MySQL stores transactional entities. MongoDB stores AI documents. The codebase supports Kafka and Redis integration, but the current Heroku deployment avoids those paid add-ons and uses the worker's database polling path instead.

## Why this is a good Codex challenge entry

- The repo shows real engineering work, not only prompt wrappers.
- The app solves a real user problem with a visible workflow.
- The architecture uses production-style boundaries and async processing.
- The submission package explains both the product value and the implementation choices.

## If you want to run it locally

Use [DEMO.md](DEMO.md). The local `dev` profile is the supported path.
