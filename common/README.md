# common

Shared contracts and cross-module models.

For platform context and architecture, see the [root README](../README.md).

## Purpose

`common` centralizes reusable types consumed by both `resume-api` and `resume-worker`.

## Responsibilities

- Shared Kafka payload contracts (for AI jobs)
- Shared DTO-like records and utility models
- Reduce duplication and keep integration contracts consistent

## Key Dependencies

- Java 17
- Spring Boot base dependencies

## Usage

Imported as a module dependency by:
- `resume-api`
- `resume-worker`

## Tests

From repo root:

```bash
./mvnw -pl common test
```

Or inside module:

```bash
./mvnw test
```

## Notes

- Backward compatibility of shared contracts should be treated carefully, especially Kafka message schemas.
