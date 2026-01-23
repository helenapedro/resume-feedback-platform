# Local Kafka (Docker Desktop)

Use these instructions to run a local Kafka broker so the `resume-api` can
publish AI job events.

## 1) Start Kafka

Create a `docker-compose.kafka.yml` file in any folder (for example, in the repo root):

```yaml
services:
  kafka:
    image: bitnami/kafka:3.7
    ports:
      - "9092:9092"
    environment:
      - KAFKA_ENABLE_KRAFT=yes
      - KAFKA_CFG_NODE_ID=1
      - KAFKA_CFG_PROCESS_ROLES=broker,controller
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
      - ALLOW_PLAINTEXT_LISTENER=yes
```

Then run:

```bash
docker compose -f docker-compose.kafka.yml up -d
```

If you need to stop and restart it, you can use:

```bash
docker compose -f docker-compose.kafka.yml down -v
docker compose -f docker-compose.kafka.yml up -d
```

## 2) Confirm the broker

```bash
docker compose -f docker-compose.kafka.yml ps
```

You should see the `kafka` container with status `Up`.

## 3) Configure the resume-api

In the `dev` profile, `resume-api` already points to `localhost:9092` via
`spring.kafka.bootstrap-servers`. If you need to change the host or port, edit:

```
feedback/resume-api/src/main/resources/application-dev.yml
```

## 4) Start the resume-api

```bash
cd feedback
./mvnw -pl resume-api -am spring-boot:run
```

When a job is created or regenerated, the service publishes an event to the
`resume-ai-jobs` topic (configurable).

## Troubleshooting

### Maven cannot find `resume-api` in the reactor

If you run the command from inside `feedback/resume-api`, Maven will not see the
multi-module parent. Always run it from the `feedback` folder:

```bash
cd feedback
./mvnw -pl resume-api -am spring-boot:run
```

### Missing `com.pedro.common.ai` or `KafkaProperties`

Those errors indicate that the shared `common` module is not built (or its
dependencies are not on the classpath). The `-am` flag builds required modules
automatically. If needed, you can also install the module directly:

```bash
cd feedback
./mvnw -pl common install
./mvnw -pl resume-api -am spring-boot:run
```
