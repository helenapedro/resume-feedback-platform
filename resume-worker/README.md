# resume-worker

Asynchronous worker module for AI job processing.

For platform context and architecture, see the [root README](../README.md).

## Purpose

`resume-worker` consumes AI job events from Kafka, generates resume feedback (Gemini), stores outputs, and updates job state.

## Responsibilities

- Consume `AiJobRequestedMessage` events from Kafka
- Transition job lifecycle (`PENDING -> PROCESSING -> DONE/FAILED`)
- Generate AI feedback from prompt payloads
- Persist AI feedback documents in MongoDB
- Persist feedback references and job state in MySQL
- Retry failed jobs using scheduled backoff policy

## Key Dependencies

- Spring Boot
- Spring Kafka
- Spring Data JPA (MySQL)
- Spring Data MongoDB
- Spring Boot Actuator

## Local Run

From repo root:

```bash
./mvnw -pl resume-worker spring-boot:run
```

## Configuration (Important)

Core variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_MONGODB_URI`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_PREFIX` (optional)

Gemini:

- `GEMINI_API_KEY` (required for real AI responses)
- `app.ai-feedback.gemini.enabled` (default true in current config)
- `app.ai-feedback.gemini.model`
- `app.ai-feedback.gemini.temperature`
- `app.ai-feedback.gemini.max-output-tokens`

## Runtime Behavior

- Topic: `${KAFKA_PREFIX}resume-ai-jobs` (default)
- Consumer group: `${KAFKA_PREFIX}resume-worker` (default)
- Failed jobs are retried on schedule until max attempts are reached.

## Tests

```bash
./mvnw -pl resume-worker test
```

## Notes

- If `GEMINI_API_KEY` is missing or Gemini fails, fallback feedback content may be generated depending on current implementation.
- Monitor logs for consumer errors and job status transitions.
