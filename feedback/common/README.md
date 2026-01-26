# common

Shared foundation module for the Resume Feedback Platform. This module is intended to
hold cross-cutting abstractions (shared DTOs, error models, utilities, etc.) that are
reused by `resume-api` and `resume-worker`, including Kafka message payloads for AI jobs.

## Tech Stack
- Java 17 + Spring Boot

## Local Development
Run tests from this module:

```bash
./mvnw test
```

Or from the `feedback` root:

```bash
./mvnw -pl common test
```
