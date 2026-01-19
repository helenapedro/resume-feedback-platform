# resume-api

Backend service for resume upload/versioning, secure sharing, and AI job tracking.

## Tech
- Java + Spring Boot
- MySQL (metadata + security/audit)
- Local storage (dev) / S3 (future)
- JWT authentication

## Run (dev)
1) Start DB

```bash
cd docker
docker compose up -d

2) Run API
Run ResumeApiApplication with profile dev
```
## Auth
### Register
- POST /api/auth/register

Body:
```json
{"email":"pedro@local.dev","password":"123456"}
```

Response:
```json
{"accessToken":"..."}
```
### Login
- POST /api/auth/login

Body:
```json
{"email":"pedro@local.dev","password":"123456"}
```
Response:
```json
{"accessToken":"..."}
```
### Resumes
- GET /api/resumes (JWT)
- POST /api/resumes (multipart, JWT)
- POST /api/resumes/{id}/versions (multipart, JWT)
- GET /api/resumes/{id} (JWT)
- GET /api/resumes/{resumeId}/versions/{versionId}/download (JWT)

## Share Links

#### Owner (JWT):
- POST /api/resumes/{resumeId}/share-links 
- GET /api/resumes/{resumeId}/share-links 
- POST /api/resumes/{resumeId}/share-links/{linkId}/revoke

#### Public (token):
- GET /api/share/{token} (metadata do link)
- GET /api/share/{token}/download (download current version)
exemplo curl:
```bash
curl -L -o resume.pdf http://localhost:8080/api/share/<TOKEN>/download
```

#### DB schema
Tables:
- access_audit
- ai_feedback_refs
- ai_jobs
- comments
- resume_versions,
- resumes
- share_links,
- users

#### Notes
- Tokens are stored only as SHA-256 hashes. 
- Share link returns token plaintext only once at creation.