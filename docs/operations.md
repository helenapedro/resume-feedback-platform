# Operations

Related docs:
- [Requirements](requirements.md)
- [Architecture](architecture.md)
- [Root README](../README.md)

## Local Development

### Prerequisites

- Java 17
- Maven Wrapper (`mvnw` / `mvnw.cmd`)
- Docker Desktop (recommended for local infra)

### Local Infrastructure with Docker Desktop

From repo root:

```bash
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.kafka.yml up -d
```

This brings up local dependencies used during development (MySQL/Redis and Kafka stack).
If you use local Mongo as well, run it in Docker or provide `SPRING_DATA_MONGODB_URI`.

### Build

```bash
./mvnw -DskipTests package
```

### Run Services

Run API:

```bash
./mvnw -pl resume-api spring-boot:run -Dspring-boot.run.profiles=dev
```

Run worker:

```bash
./mvnw -pl resume-worker spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile uses local defaults for:

- MySQL: `jdbc:mysql://localhost:3306/resume_feedback`
- MongoDB: `mongodb://localhost:27017/resume_feedback`
- Kafka: `localhost:9092`
- API JWT settings and local file storage under `./var/resumes`

Production runs should continue to rely on environment variables. The current hosted deployment does not require paid Heroku Kafka or Redis add-ons.

## Configuration

### Core Environment Variables

| Variable | Required | Module | Notes |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Yes | API/Worker | MySQL connection |
| `SPRING_DATASOURCE_USERNAME` | Yes | API/Worker | MySQL user |
| `SPRING_DATASOURCE_PASSWORD` | Yes | API/Worker | MySQL password |
| `SPRING_DATA_MONGODB_URI` | Yes | API/Worker | MongoDB URI |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | No | API/Worker | Needed only when Kafka integration is enabled |
| `GEMINI_API_KEY` | Yes (AI feedback) | Worker | Gemini API key |
| `KAFKA_PREFIX` | No | API/Worker | Optional topic/group prefix |

### Kafka SSL (Optional Integration)

Worker and API support Heroku-style Kafka SSL properties through:

- `SPRING_KAFKA_PROPERTIES_SSL_KEYSTORE_KEY`
- `SPRING_KAFKA_PROPERTIES_SSL_KEYSTORE_CERTIFICATE_CHAIN`
- `SPRING_KAFKA_PROPERTIES_SSL_TRUSTSTORE_CERTIFICATES`

With fallback to legacy `KAFKA_*` variables where configured.

## Testing

Run all tests:

```bash
./mvnw test
```

Run module tests:

```bash
./mvnw -pl resume-api test
./mvnw -pl resume-worker test
./mvnw -pl common test
```

## Deployment

- Primary flow: GitHub `main` -> Heroku automatic deploy
- Process types are defined in `Procfile` (`web` and `worker`)
- Prefer committing directly on `main` for release consistency (current team workflow)

### Current Production Topology

- Frontend: React + TypeScript app hosted on AWS Amplify
- App runtime: Heroku (`web` + `worker`)
- Relational database: MySQL
- Document database: MongoDB
- Messaging/cache: Kafka and Redis are supported in code, but not used in the current low-cost Heroku deployment

Recommended release validation:

1. `POST /api/auth/login`
2. `POST /api/resumes` (multipart)
3. `GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest`

## Monitoring and Troubleshooting

### What to monitor

- API health and startup errors
- Worker consumption and job transitions
- AI job statuses: `PENDING`, `PROCESSING`, `DONE`, `FAILED`

### Typical checks

- Ensure worker is up and polling pending AI jobs
- If Kafka is enabled, verify consumer group authorization
- Verify `GEMINI_API_KEY` is set for worker
- Inspect failed jobs (`errorCode`, `errorDetail`) for root cause

### Common failure patterns

- Serialization issues (for example `Instant`) in Kafka payloads when Kafka is enabled
- Event published before transaction commit (resolved by after-commit publication)
- Broker SSL/auth misconfiguration when Kafka is enabled
