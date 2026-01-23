# resume-worker

Background worker service for asynchronous processing (AI feedback generation, job
tracking, and queue consumption). This module is currently a Spring Boot skeleton that
will grow as the async pipeline is implemented.

## Tech Stack
- Java 17 + Spring Boot
- Spring Data JPA
- Spring Data MongoDB
- Spring Boot Actuator

## Local Development
Run the worker locally (from `feedback/resume-worker`):

```bash
./mvnw spring-boot:run
```

If you need to run only module tests from the `feedback` root:

```bash
./mvnw -pl resume-worker test
```
