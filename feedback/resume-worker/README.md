# resume-worker

Background worker service for asynchronous processing (AI feedback generation, job
tracking, and queue consumption). This module consumes AI job events from Kafka,
persists AI feedback in MongoDB, and updates job status in MySQL.

## Tech Stack
- Java 17 + Spring Boot
- Spring Data JPA
- Spring Data MongoDB
- Spring Boot Actuator

## Local Development
Start infrastructure (from `feedback/docker`):

```bash
docker compose up -d
```

Start Kafka using the local guide in `docs/kafka-local.md`, and MongoDB using
`docs/mongodb-local.md`, then run the worker locally (from
`feedback/resume-worker`):

```bash
./mvnw spring-boot:run
```

If you need to run only module tests from the `feedback` root:

```bash
./mvnw -pl resume-worker test
```
