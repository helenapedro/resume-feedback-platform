# Operations

## Local Development

### Prerequisites

- Java 17
- Maven Wrapper (`mvnw` / `mvnw.cmd`)
- MySQL
- MongoDB
- Kafka
- Redis

### Build

```bash
./mvnw -DskipTests package
```

### Run Services

Run API:

```bash
./mvnw -pl resume-api spring-boot:run
```

Run worker:

```bash
./mvnw -pl resume-worker spring-boot:run
```

## Configuration

### Core Environment Variables

| Variable | Required | Module | Notes |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Yes | API/Worker | MySQL connection |
| `SPRING_DATASOURCE_USERNAME` | Yes | API/Worker | MySQL user |
| `SPRING_DATASOURCE_PASSWORD` | Yes | API/Worker | MySQL password |
| `SPRING_DATA_MONGODB_URI` | Yes | API/Worker | MongoDB URI |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Yes | API/Worker | Kafka brokers |
| `GEMINI_API_KEY` | Yes (AI feedback) | Worker | Gemini API key |
| `KAFKA_PREFIX` | No | API/Worker | Optional topic/group prefix |

### Kafka SSL (Production Pattern)

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

- Ensure worker is up and subscribed to AI jobs topic
- Verify Kafka consumer group authorization
- Verify `GEMINI_API_KEY` is set for worker
- Inspect failed jobs (`errorCode`, `errorDetail`) for root cause

### Common failure patterns

- Serialization issues (for example `Instant`) in Kafka payloads
- Event published before transaction commit (resolved by after-commit publication)
- Broker SSL/auth misconfiguration

