# resume-worker

Asynchronous worker module for AI job processing.

For platform context and architecture, see the [root README](../README.md).

## Purpose

`resume-worker` processes AI jobs in the background, generates resume feedback (Gemini), stores outputs, generates version-to-version progress analysis when possible, and updates job state.

## Responsibilities

- Process pending AI jobs in the background
- Transition job lifecycle (`PENDING -> PROCESSING -> DONE/FAILED`)
- Generate AI feedback from prompt payloads
- Generate AI progress analysis for newer resume versions using the previous version plus prior AI feedback as baseline context
- Persist AI feedback documents in MongoDB
- Persist feedback/progress references and job state in MySQL
- Retry failed jobs using scheduled backoff policy

## Key Dependencies

- Spring Boot
- Spring Kafka (optional integration)
- Spring Data JPA (MySQL)
- Spring Data MongoDB
- Spring Boot Actuator

## Local Run

From repo root:

```bash
./mvnw -pl resume-worker spring-boot:run -Dspring-boot.run.profiles=dev
```

## Configuration (Important)

Core variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_MONGODB_URI`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` (only if Kafka is enabled)
- `KAFKA_PREFIX` (optional)

Gemini:

- `GEMINI_API_KEY` (required for real AI responses)
- `app.ai-feedback.gemini.enabled` (default true in current config)
- `app.ai-feedback.gemini.model`
- `app.ai-feedback.gemini.temperature`
- `app.ai-feedback.gemini.max-output-tokens`
- `app.ai-feedback.max-resume-chars`
- `app.ai-feedback.progress-enabled`

Cost-control environment overrides:

- `APP_AI_FEEDBACK_MAX_RESUME_CHARS` limits resume text sent to Gemini. Default: `8000`. Progress analysis uses half of this value per resume version, with a minimum of `1500`, and sends head/tail excerpts instead of two full resume bodies.
- `APP_AI_FEEDBACK_GEMINI_MAX_OUTPUT_TOKENS` limits generated JSON size. Default: `1800`. Avoid setting this too low; truncated JSON causes failed jobs and can cost more through retries.
- `APP_AI_FEEDBACK_GEMINI_TEMPERATURE` lowers variation. Default: `0.2`.
- `APP_AI_FEEDBACK_PROGRESS_ENABLED=false` disables version-to-version progress calls. This can roughly halve AI calls for second and later resume versions.
- `APP_AI_JOBS_RETRY_MAX_ATTEMPTS` caps failed-job retries. Default: `3`.

## Runtime Behavior

- If Kafka is enabled, topic: `${KAFKA_PREFIX}resume-ai-jobs` and consumer group: `${KAFKA_PREFIX}resume-worker`
- The scheduler polls `PENDING` jobs only when Kafka is disabled. When Kafka is enabled, it only retries due failed jobs.
- Failed jobs are retried on schedule until max attempts are reached.
- Progress analysis is skipped for the first version of a resume, or when the previous version has no stored baseline feedback yet.

## Tests

```bash
./mvnw -pl resume-worker test
```

## Notes

- If `GEMINI_API_KEY` is missing or Gemini fails, fallback feedback content may be generated depending on current implementation.
- Monitor logs for worker polling, processing, and job status transitions.
- Progress-analysis failures do not fail the primary feedback job; the worker logs and skips that secondary artifact.
