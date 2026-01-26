# resume-feedback-platform

### Architecture Diagram (Data Flow)

High-level data flow across the API, Kafka, worker, and persistence layers, including storage for AI feedback documents and job metadata.
![Architecture and data flow diagram](https://resume-feedback-platform.s3.us-east-1.amazonaws.com/archtectureanddataflow.png)

### LLM Integration (Google Gemini)

The AI feedback pipeline is designed to plug into Google Gemini from the worker:
- `resume-api` creates an AI job and publishes it to Kafka.
- `resume-worker` consumes the job, fetches or derives resume content, and calls Gemini.
- The generated feedback is stored in MongoDB and linked in MySQL.

Implementation details and setup steps are documented in [docs/llm-gemini.md](./llm-gemini.md).

### Use Case Diagram

This diagram captures the primary actors (resume owners and external reviewers) and the system boundaries, showing how authentication, resume management, and sharing flows map to user-facing capabilities and external access paths.
![Use cases](./diagrams/use-cases.jpg)

### User Stories

This view translates stakeholder needs into scoped stories, highlighting the backlog shape across registration, resume lifecycle, sharing, and feedback workflows that the platform must satisfy.

![User stories](./diagrams/user-stories.jpg)


### Domain Class Model

The model outlines core entities (User, Resume, ResumeVersion, SharedLink, Comment, AIJob) and their relationships, mention many-to-one relationships (e.g., many versions to one resume) to clarify the data structure.

![Domain class](https://resume-feedback-platform.s3.us-east-1.amazonaws.com/domainmodel.png)

#### Domain Class Model (PlantUML)

```plantuml
@startuml
title Resume Feedback Platform - Domain Class Model (Updated)
skinparam classAttributeIconSize 0

class User {
  UUID id
  String email
  String passwordHash
  Role role
  boolean enabled
  Instant createdAt
  Instant lastLoginAt
}

enum Role {
  USER
  ADMIN
}

class Resume {
  UUID id
  String title
  Instant createdAt
}

class ResumeVersion {
  UUID id
  int versionNumber
  String originalFilename
  String fileName
  String contentType
  Long fileSizeBytes
  String storageKey
  String s3Bucket
  String s3ObjectKey
  String s3VersionId
  String checksumSha256
  Instant createdAt
}

class ShareLink {
  UUID id
  String tokenHash
  Permission permission
  Instant expiresAt
  Instant revokedAt
  Integer maxUses
  int useCount
  Instant createdAt
}

enum Permission {
  VIEW
  COMMENT
}

class Comment {
  UUID id
  String authorLabel
  String body
  String anchorRef
  Instant createdAt
  Instant updatedAt
}

class AccessAudit {
  UUID id
  EventType eventType
  String ipAddress
  String userAgent
  boolean success
  String failureReason
  Instant occurredAt
}

enum EventType {
  OPEN_LINK
  DOWNLOAD
  DOWNLOAD_ATTEMPT
  DOWNLOAD_GRANTED
  DOWNLOAD_DENIED
  COMMENT_ATTEMPT
  COMMENT_CREATED
  COMMENT_DENIED
}

class AiJob {
  UUID id
  Status status
  int attemptCount
  String idempotencyKey
  Instant createdAt
  Instant updatedAt
  Instant startedAt
  Instant finishedAt
  String errorCode
  String errorDetail
  Instant nextRetryAt
}

enum Status {
  PENDING
  PROCESSING
  DONE
  FAILED
}

class AiFeedbackRef {
  UUID id
  int feedbackVersion
  String mongoDocId
  String model
  String promptVersion
  Instant createdAt
}

class AiFeedbackDocument <<MongoDB>> {
  String id
  UUID jobId
  UUID resumeId
  UUID resumeVersionId
  UUID ownerId
  Instant createdAt
  String model
  String promptVersion
  String summary
  List strengths
  List improvements
}

User "1" --> "0..*" Resume : owns
Resume "1" --> "0..*" ResumeVersion : versions
Resume "0..1" --> "1" ResumeVersion : currentVersion
Resume "1" --> "0..*" ShareLink : shareLinks
ResumeVersion "1" --> "0..*" Comment : comments
Comment "0..1" --> "0..*" Comment : replies
ResumeVersion "1" --> "0..*" AiJob : aiJobs
ResumeVersion "1" --> "0..*" AiFeedbackRef : feedbackRefs
AiFeedbackRef "1" --> "1" AiFeedbackDocument : pointsTo
ShareLink "1" --> "0..*" AccessAudit : accessEvents
AccessAudit "1" --> "1" Resume : resume
AccessAudit "0..1" --> "1" ResumeVersion : resumeVersion
ShareLink "0..1" --> "1" User : createdBy
ResumeVersion "0..1" --> "1" User : createdBy
Comment "0..1" --> "1" User : authorUser

@enduml
```


## Sequence Diagrams

Sequence 01 traces the authenticated resume upload flow, emphasizing request validation, metadata persistence, version creation, and downstream AI job enqueueing.
![Seq 01](./diagrams/seq-01.jpg)


Sequence 02 details how a shareable link is created and stored, including token generation, expiring tokens, and the response returned to the owner.
![Seq 02.drawio](./diagrams/seq-02.drawio.png)


Sequence 03 models external access via a shared link, focusing on token validation, permission checks, and secure retrieval of the current resume version.
![Seq 03](./diagrams/seq-03.jpg)


Sequence 04 covers the commenting flow, showing how viewer permissions are enforced and comments are persisted against a specific resume version.
![Seq 04](./diagrams/seq-04.jpg)


Sequence 05 follows the AI feedback pipeline, from job creation through asynchronous processing and storage of generated feedback.
![Seq 05](./diagrams/seq-05.jpg)


Sequence 06 illustrates resume version history access, including pagination of versions and retrieval of historical files for authorized owners.
![Seq 06](./diagrams/Seq-06.jpg)


## Domain Behavior

The state diagrams summarize lifecycle transitions for resumes, shared links, and AI jobs, capturing terminal states and error paths needed for robust orchestration.
![State diagrams](./diagrams/statediagrams.jpg)
