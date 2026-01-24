# Redis (local)

The API uses Redis-backed rate limiting for the public `/api/share/**` endpoints. Run Redis
locally when testing the API or running multiple instances behind a load balancer.

## Docker (recommended)

From the repository root:

```bash
docker compose -f feedback/docker/docker-compose.yml up -d redis
```

This exposes Redis on `localhost:6379`.

## Configuration

`resume-api` reads the connection details from `spring.data.redis.*`:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

These settings are in `feedback/resume-api/src/main/resources/application.yml` by default.

## Production note

If Redis is not provided as a managed service, run it via Docker on the production host and
point `spring.data.redis.*` at that instance.
