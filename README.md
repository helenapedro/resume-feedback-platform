# resume-feedback-platform
Enterprise-grade resume feedback platform that allows users to upload and version resumes, share them securely via expiring links, receive AI-generated feedback asynchronously, and collaborate through versioned comments. Built with Spring Boot, JPA/Hibernate, MongoDB, AWS S3, and message-driven architecture.

## System Requirements
### 1. Functional Requirements
#### 1.1 User Management
- The system shall allow users to register and authenticate securely.
- The system shall allow authenticated users to manage their own resumes.
- The system shall enforce access control for all owner-level operations.

#### 1.2 Resume Management
- The system shall allow each user to upload multiple resumes.
- Each resume shall represent a logical document owned by a user.
- The system shall allow a resume to be updated, creating a new version.
- The system shall store resume metadata in a relational database.
- The system shall store resume files in AWS S3 using object versioning.

#### 1.3 Resume Versioning
- Each resume shall have one or more versions.
- Each version shall represent a distinct uploaded file.
- The system shall track resume version numbers sequentially.
- The system shall identify one version as the current version.
- The system shall allow resume owners to view and download previous versions.
- Resume version metadata shall include creation timestamp and storage reference.

#### 1.4 Resume Sharing via Secure Link
- The system shall allow a resume owner to generate a shareable link.
- Each shared link shall be associated with a resume and its current version.
- Shared links shall be protected using secure, unguessable tokens.
- Shared links shall support permission levels (VIEW or COMMENT).
- Shared links shall include an expiration date and time.
- The system shall allow resume owners to revoke shared links at any time.
- When a shared link expires or is revoked, further access shall be denied.

#### 1.5 Resume Viewing
- Resume owners shall be able to view all versions of their resumes.
- External users shall be able to view only the current version of a resume via a valid shared link.
- Resume files shall be accessed through time-limited presigned URLs generated after authorization.

#### 1.6 Commenting System
- The system shall allow resume owners to add comments to any resume version.
- The system shall allow external users with COMMENT permission to add comments to the current resume version.
- All comments shall be permanently stored and associated with a specific resume version.
- Comments created via shared links shall remain visible to the resume owner after link expiration.
- Comments shall be displayed in chronological order per resume version.

#### 1.7 AI-Powered Resume Feedback
- The system shall automatically trigger AI feedback generation for each new resume version.
- AI feedback processing shall be performed asynchronously via a message queue.
- The system shall associate AI feedback with the specific resume version.
- The system shall store AI feedback as versioned documents in MongoDB.
- The system shall allow resume owners to request regeneration of AI feedback.

#### 1.8 AI Job Processing and Status Tracking
- The system shall track AI feedback jobs with statuses (PENDING, PROCESSING, DONE, FAILED).
- Each AI job shall be associated with a specific resume version.
- The system shall expose AI job status to the resume owner.
- The system shall support retry mechanisms for failed AI jobs.

#### 1.9 Auditing and Access Logging
- The system shall record access events when a shared link is used.
- The system shall log resume uploads, version creation, link generation, commenting, and AI processing events.
- Audit logs shall support traceability for security and debugging purposes.

### 2. Non-Functional Requirements
#### 2.1 Security
- The system shall require authentication for all resume owner operations.
- Shared access shall be authorized exclusively through secure token validation.
- Shared tokens shall be stored as hashed values in the database.
- Resume files stored in S3 shall not be publicly accessible.
- Presigned URLs shall be generated only after successful authorization.
- The system shall apply rate limiting to shared-link endpoints.

#### 2.2 Performance
- Resume upload and metadata persistence shall complete within acceptable response times.
- AI feedback processing shall not block user-facing requests.
- The system shall efficiently handle concurrent access to resumes and shared links.
- Comment and version listings shall support pagination.

#### 2.3 Scalability
- The system shall support horizontal scaling of the API layer.
- AI processing shall scale independently via message queue consumers.
- The system architecture shall support growth in users, resumes, versions, and AI jobs.

#### 2.4 Reliability and Fault Tolerance
- The system shall ensure consistency between resume version creation and AI job registration.
- Failures in AI feedback generation shall not impact resume access functionality.
- The system shall support retry and recovery mechanisms for asynchronous processing.
- The system shall tolerate temporary unavailability of external AI services.

#### 2.5 Maintainability
- The system shall follow a layered architecture (Controller, Service, Repository).
- Cross-cutting concerns such as logging and auditing shall be implemented using AOP.
- Business logic shall be isolated from infrastructure and persistence concerns.
- The system shall be modular and easy to extend.

#### 2.6 Observability and Monitoring
- The system shall expose health and metrics endpoints.
- The system shall provide structured logging with correlation identifiers.
- The system shall track metrics related to AI job execution and failures.

#### 2.7 Data Consistency

- The relational database shall serve as the source of truth for users, resumes, versions, and permissions.
- MongoDB shall be used exclusively for semi-structured AI feedback documents.
- The system shall avoid distributed transactions and rely on controlled eventual consistency.

#### 2.8 Testing
- The system shall include unit tests for business services.
- The system shall include repository tests for relational and MongoDB access.
- The system shall include integration tests for REST APIs.
- The system shall include tests for asynchronous AI job processing and failure handling.
