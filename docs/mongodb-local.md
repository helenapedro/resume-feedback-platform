# MongoDB (Docker Desktop) Setup

Use this guide to run MongoDB locally with Docker Desktop so `resume-worker` can
persist AI feedback documents.

## 1) Start MongoDB

From the repo root, start the infrastructure services:

```bash
cd feedback/docker
docker compose up -d
```

This uses the `mongo` service defined in:

```
feedback/docker/docker-compose.yml
```

By default, MongoDB is exposed on `localhost:27017`.

## 2) Confirm the container is running

```bash
docker compose ps
```

You should see the `mongo` container with status `Up`.

## 3) Verify the database connection string

The worker connects using the `prod` profile (default) with the following URI:

```
mongodb://localhost:27017/resume_feedback_ai
```

You can confirm (or change) it in:

```
feedback/resume-worker/src/main/resources/application-prod.yml
```

## 4) Run the worker

From `feedback/resume-worker`:

```bash
./mvnw spring-boot:run
```

## 5) Troubleshooting

### Port already in use

If `27017` is already in use, change the host port in `docker-compose.yml`, for example:

```yaml
services:
  mongo:
    ports:
      - "27018:27017"
```

Then update the worker URI to:

```
mongodb://localhost:27018/resume_feedback_ai
```

### Reset data

To wipe local data:

```bash
docker compose down -v
docker compose up -d
```
