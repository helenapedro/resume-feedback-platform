# Requirements

## Functional Requirements

### Core Capabilities

- User registration and login with JWT authentication
- Resume upload, listing, download, and versioning
- Secure share-link creation, listing, and revocation
- Owner and token-based public comments
- Asynchronous AI job creation and status tracking
- AI feedback retrieval for resume versions

### Main Use Cases

1. User authenticates and uploads a resume.
2. User creates new resume versions over time.
3. User creates share links for external reviewers.
4. Reviewer accesses shared resume and optionally comments (if permitted).
5. System creates AI feedback jobs and processes them asynchronously.
6. User checks AI job status and retrieves generated feedback.

### Core Business Rules

- Only resource owners can manage private resume assets and owner endpoints.
- Share tokens are returned in plaintext only at creation time and stored hashed.
- AI jobs are idempotent for default creation flow (version-based key).
- AI processing status is explicit (`PENDING`, `PROCESSING`, `DONE`, `FAILED`).
- Failed AI jobs may retry according to configured retry policy.

### Happy Path

1. Upload resume (or version).
2. Persist metadata in MySQL and content in configured storage.
3. Create AI job and publish Kafka event after DB commit.
4. Worker consumes event, generates feedback, stores result, updates status.
5. API returns job status and latest feedback.

### Alternative/Error Flows

- Duplicate/idempotent AI job request returns existing job.
- Kafka/serialization/provider issues move job to `FAILED` with error details.
- Worker retry scheduler reprocesses due failed jobs up to max attempts.
- Unauthorized access to owner endpoints returns forbidden errors.

## Non-Functional Requirements

### Performance

- User-facing API flow should not block on LLM response time.
- Asynchronous architecture should keep upload latency low under AI load.

### Scalability

- API and worker should scale horizontally as stateless services.
- Kafka partitioning enables parallel job consumption.

### Availability and Reliability

- Decoupled API/worker pipeline tolerates temporary AI provider latency/failures.
- Retry strategy for failed jobs with configurable backoff.

### Data Consistency

- Strong consistency for transactional entities in MySQL.
- Eventual consistency between job creation and feedback availability.

### Security

- JWT auth for protected routes.
- Authorization checks for owner-specific resources.
- Tokenized public access with revocation support.
- TLS-secured Kafka connectivity in production configuration.

### Observability

- Structured application logs for API and worker.
- Health endpoints through Spring Actuator.
- Persisted AI job error metadata for operational diagnosis.

### Maintainability

- Multi-module separation (`resume-api`, `resume-worker`, `common`).
- Shared contract models centralized in `common`.
- Clear split between synchronous request handling and async processing.

### Extensibility

- AI provider integration isolated in worker-side client/factory layers.
- Event-driven flow can be extended with additional consumers/workflows.

