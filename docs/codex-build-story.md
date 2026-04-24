# Built With Codex

This project is not presented as "AI generated code." It is presented as a real engineering workflow where Codex accelerated delivery, debugging, and packaging.

## Where Codex added value

- Refined the multi-module Maven setup so running individual modules also builds required dependencies.
- Hardened local development with explicit `dev` profiles and local storage support.
- Fixed startup and dependency issues across `resume-api`, `resume-worker`, and `common`.
- Tightened the async AI handoff path by ensuring publish-after-commit behavior is correct when Kafka is enabled.
- Added focused integration and worker tests around the highest-risk paths.
- Reworked the submission docs so the hosted app, demo path, and product story are obvious.

## Concrete examples

- `AiJobService` now flushes idempotent job creation and supports reliable after-commit publication behavior, which makes the async pipeline behavior consistent under JPA transaction timing.
- Share-link integration tests now verify both access control and audit behavior.
- Worker tests cover both successful processing and retry scheduling on failures.
- Local development now supports `LOCAL` file storage instead of assuming S3.

## Why that matters for the challenge

The strongest Codex entries are not just "I asked for code." They show that Codex helped move a real project forward:

- faster debugging
- faster iteration on architecture
- cleaner packaging for evaluators
- more confidence through targeted automated tests

## Recommended framing in the submission

When describing how Codex helped, keep it specific:

- "Codex helped me debug module wiring and local runtime configuration."
- "Codex helped harden the async AI pipeline and add focused tests."
- "Codex helped turn the repo into a submission-ready package with a clear judge path."
