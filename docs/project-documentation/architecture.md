# Architecture

Related docs:
- [Requirements](requirements.md)
- [Operations](operations.md)
- [Root README](../README.md)

## Architectural Style

- Layered modular monolith at repository level
- Asynchronous worker processing for the AI pipeline, with optional event-driven Kafka support

## High-Level Components

![Overall architecture diagram](../project-images/overall-architecture-diagram.png)

- `resume-api`
  - Auth, resume, sharing, comments, AI job orchestration
  - Creates `AiJob` records and supports optional Kafka publishing
- `resume-worker`
  - Processes AI jobs, retrieves optional Microsoft IQ / Foundry IQ grounding context, calls the configured AI provider, stores feedback, updates job status
- `common`
  - Shared message contracts and models
- Datastores
  - MySQL: users, resumes, versions, jobs, audit, refs
  - MongoDB: AI feedback documents
  - Redis: optional rate-limit support
  - Kafka: optional AI jobs topic integration

## Data Flow

![Async upload and AI feedback pipeline](../project-images/resume-feedback-async-upload-ai-pipeline.png)

```mermaid
flowchart LR
  U[User / Client] --> API[resume-api]
  API --> MYSQL[(MySQL)]
  API --> WORKER[resume-worker]
  API -. optional .-> KAFKA[(Kafka topic: resume-ai-jobs)]
  KAFKA -. optional .-> WORKER
  WORKER -. optional grounding .-> FOUNDRYIQ[Microsoft IQ / Foundry IQ Grounding<br/>Azure AI Search / Local Knowledge]
  WORKER --> PROVIDER[AI Provider Registry<br/>Gemini / OpenAI / Azure OpenAI]
  WORKER --> MONGO[(MongoDB)]
  WORKER --> MYSQL
  API --> MONGO
  API --> U
```

## AI Sequence Flows

### AI Resume Feedback Generation

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant API as resume-api
    participant MySQL as MySQL
    participant Worker as resume-worker
    participant Extractor as ResumeTextExtractor
    participant Grounding as Microsoft IQ Grounding
    participant Provider as Configured AI Provider
    participant Mongo as MongoDB

    User->>API: POST /api/resumes or POST /versions
    API->>MySQL: Save Resume + ResumeVersion
    API->>MySQL: Create AiJob(status=PENDING)
    API-->>User: Return resume/version response

    Note over API,Worker: Current hosted deployment uses background polling from MySQL
    Note over API,Worker: Kafka publishing remains available as an optional integration path

    Worker->>MySQL: Poll pending AiJobs
    MySQL-->>Worker: PENDING job + ResumeVersion metadata

    Worker->>MySQL: Mark job PROCESSING
    Worker->>Extractor: extract(resumeVersion)

    alt Resume stored in S3
        Extractor->>Extractor: Download PDF bytes from S3
    else Resume stored locally
        Extractor->>Extractor: Read PDF from storageKey
    end

    Extractor->>Extractor: Extract and normalize PDF text
    Extractor-->>Worker: Resume text

    opt Foundry IQ grounding enabled
        Worker->>Grounding: Retrieve cited resume-review knowledge
        Grounding-->>Worker: Grounding context with source citations
    end

    Worker->>Provider: Prompt with resume text and optional grounding context
    Provider-->>Worker: JSON feedback(summary, strengths, improvements)

    alt Provider response valid
        Worker->>Mongo: Save AiFeedbackDocument
        Mongo-->>Worker: mongoDocId
        Worker->>MySQL: Save AiFeedbackRef(version, model, promptVersion, mongoDocId)
        Worker->>MySQL: Mark job DONE
    else Provider failure / parse error / provider error
        Worker->>MySQL: Mark job FAILED with error metadata
    end

    User->>API: GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest
    API->>MySQL: Read latest AiJob
    API-->>User: Job status

    User->>API: GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback
    API->>MySQL: Read latest AiFeedbackRef
    API->>Mongo: Read AiFeedbackDocument
    API-->>User: Feedback JSON
```

### AI Progress Analysis Across Resume Versions

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant API as resume-api
    participant MySQL as MySQL
    participant Worker as resume-worker
    participant Extractor as ResumeTextExtractor
    participant Grounding as Microsoft IQ Grounding
    participant Provider as Configured AI Provider
    participant Mongo as MongoDB

    User->>API: POST /api/resumes/{resumeId}/versions
    API->>MySQL: Save new ResumeVersion
    API->>MySQL: Create AiJob(status=PENDING)
    API-->>User: Return version response

    Worker->>MySQL: Poll pending AiJobs
    MySQL-->>Worker: Pending job for current version
    Worker->>MySQL: Mark job PROCESSING

    Worker->>Extractor: Extract current resume text
    Extractor-->>Worker: Current resume text
    Worker->>Mongo: Load latest feedback for previous version
    Worker->>MySQL: Load previous ResumeVersion

    alt Previous version exists and baseline feedback exists
        Worker->>Extractor: Extract previous resume text
        Extractor-->>Worker: Previous resume text
        opt Foundry IQ grounding enabled
            Worker->>Grounding: Retrieve cited version-comparison knowledge
            Grounding-->>Worker: Grounding context with source citations
        end
        Worker->>Provider: Prompt with previous resume, current resume, previous feedback, and optional grounding context
        Provider-->>Worker: JSON progress(summary, status, score, issue lists)
        Worker->>Mongo: Save AiProgressDocument
        Mongo-->>Worker: mongoDocId
        Worker->>MySQL: Save AiProgressRef(baselineVersion, model, promptVersion, mongoDocId)
    else Baseline missing
        Worker->>Worker: Skip progress generation
    end

    Worker->>MySQL: Mark primary feedback job DONE

    User->>API: GET /api/resumes/{resumeId}/versions/{versionId}/ai-progress
    API->>MySQL: Read latest AiProgressRef
    API->>Mongo: Read AiProgressDocument
    API-->>User: Progress analysis JSON
```

## Integration Strategy

- REST for user-facing operations
- Background worker for asynchronous AI jobs
- Optional Kafka integration for event-driven deployments
- Optional Microsoft IQ / Foundry IQ grounding via Azure AI Search semantic retrieval or local demo knowledge

## Persistence Strategy

- MySQL for transactional entities and job state
- MongoDB for AI feedback payloads

## Notes

- The API supports after-commit AI event publication when Kafka is enabled.
- AI job lifecycle is tracked in MySQL while feedback payloads are stored in MongoDB.
- The current hosted deployment uses the scheduled retry/polling path that processes `PENDING` jobs directly from MySQL.
