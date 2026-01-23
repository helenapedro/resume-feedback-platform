# Kafka local (Docker Desktop)

Use estas instruções para subir um broker Kafka localmente e permitir que o
`resume-api` publique eventos de AI jobs.

## 1) Subir Kafka

Crie um arquivo `docker-compose.kafka.yml` em qualquer pasta (ex.: na raiz do repo):

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

Depois execute:

```bash
docker compose -f docker-compose.kafka.yml up -d
```

## 2) Confirmar o broker

```bash
docker compose -f docker-compose.kafka.yml ps
```

Você deve ver o container `kafka` em estado `Up`.

## 3) Configurar o resume-api

No perfil `dev`, o `resume-api` já aponta para `localhost:9092` via `spring.kafka.bootstrap-servers`.
Se você precisar alterar a porta ou host, ajuste em:

```
feedback/resume-api/src/main/resources/application-dev.yml
```

## 4) Subir o resume-api

```bash
cd feedback
./mvnw -pl resume-api spring-boot:run
```

Quando um job for criado ou regenerado, o serviço publicará um evento no tópico
`resume-ai-jobs` (configurável).
