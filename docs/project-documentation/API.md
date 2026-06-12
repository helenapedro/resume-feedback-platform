# API Reference

Backend base URL in production:

```text
https://resumefeedback-api.hmpedro.com
```

Local development default:

```text
http://localhost:8080
```

Protected endpoints require:

```http
Authorization: Bearer <accessToken>
```

## Authentication

### Register

`POST /api/auth/register`

```json
{
  "email": "user@example.com",
  "password": "strong-password"
}
```

Returns:

```json
{
  "accessToken": "<jwt>"
}
```

### Login

`POST /api/auth/login`

```json
{
  "email": "user@example.com",
  "password": "strong-password"
}
```

### Google Login

`POST /api/auth/google`

```json
{
  "idToken": "<google-id-token>"
}
```

If the email does not already exist, the backend creates the user and returns a JWT.

### Reactivate Account

`POST /api/auth/reactivate`

```json
{
  "email": "user@example.com",
  "password": "strong-password"
}
```

## User Profile

### Get Current Profile

`GET /api/users/me`

### Update Current Profile

`PATCH /api/users/me`

All fields are optional:

```json
{
  "fullName": "Helena Pedro",
  "phone": "+1 555 123 4567",
  "bio": "Backend engineer focused on resume intelligence.",
  "avatarUrl": "https://cdn.example.com/avatar.jpg"
}
```

### Upload Avatar

`POST /api/users/me/avatar`

Multipart form data:

| Field | Required | Notes |
|---|---:|---|
| `file` | Yes | `png`, `jpg`, `jpeg`, or `webp`; max size is configured by the API |

### Deactivate Account

`POST /api/users/me/deactivate`

Returns `204 No Content`.

### Delete Account

`DELETE /api/users/me`

Returns `204 No Content`.

## Resumes

### List Resumes

`GET /api/resumes`

### Get Resume Details

`GET /api/resumes/{resumeId}`

Returns the resume summary plus all versions.

### Upload First Version

`POST /api/resumes`

Multipart form data:

| Field | Required | Notes |
|---|---:|---|
| `file` | Yes | Resume PDF |
| `title` | No | Falls back to filename/title defaults when omitted |

### Add New Version

`POST /api/resumes/{resumeId}/versions`

Multipart form data:

| Field | Required | Notes |
|---|---:|---|
| `file` | Yes | New resume PDF version |

### Delete Resume

`DELETE /api/resumes/{resumeId}`

Returns `204 No Content`.

### Download Version

`GET /api/resumes/{resumeId}/versions/{versionId}/download`

Returns the file directly or redirects to a signed storage URL.

### Preview Version

`GET /api/resumes/{resumeId}/versions/{versionId}/preview`

Returns inline preview content directly or redirects to a signed storage URL.

## Share Links

### Create Share Link

`POST /api/resumes/{resumeId}/share-links`

```json
{
  "permission": "VIEW",
  "expiresAt": "2026-03-31T23:59:59Z",
  "maxUses": 20
}
```

`permission` can be `VIEW` or `COMMENT`. The plaintext token is returned only once at creation time; the backend stores only a hash.

### List Share Links

`GET /api/resumes/{resumeId}/share-links`

### Revoke Share Link

`POST /api/resumes/{resumeId}/share-links/{linkId}/revoke`

## Public Share Access

Public share endpoints use the share token and do not require owner access. Comment endpoints require authenticated users when the link permission is `COMMENT`.

### Get Shared Resume Metadata

`GET /api/share/{token}`

### Download Shared Resume

`GET /api/share/{token}/download`

### Preview Shared Resume

`GET /api/share/{token}/preview`

## Comments

### Owner Comments

`GET /api/resumes/{resumeId}/versions/{versionId}/comments`

`POST /api/resumes/{resumeId}/versions/{versionId}/comments`

`DELETE /api/resumes/{resumeId}/versions/{versionId}/comments/{commentId}`

Create body:

```json
{
  "body": "Great resume structure.",
  "anchorRef": "page:1#xywh=10,10,100,20",
  "parentCommentId": null
}
```

### Shared-Link Comments

`GET /api/share/{token}/comments`

`POST /api/share/{token}/comments`

`PATCH /api/share/{token}/comments/{commentId}`

`DELETE /api/share/{token}/comments/{commentId}`

Update body:

```json
{
  "body": "Updated comment text.",
  "anchorRef": null
}
```

## AI Feedback

AI work is asynchronous. Uploading a resume or version creates an AI job. The worker processes pending jobs, writes feedback/progress artifacts to MongoDB, and updates job state in MySQL.

### Latest AI Job

`GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest`

Job statuses:

- `PENDING`
- `PROCESSING`
- `DONE`
- `FAILED`

### Regenerate AI Feedback

`POST /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/regenerate`

Optional query parameter:

```text
language=EN|PT
```

### Fetch AI Feedback

`GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback`

Returns the latest stored feedback document for the version.

### Fetch Version Progress

`GET /api/resumes/{resumeId}/versions/{versionId}/ai-progress`

Progress exists only when the current version has a previous version with baseline AI feedback.

## Error Contract

Errors use a consistent JSON shape:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "status": 400,
  "path": "/api/example",
  "timestamp": "2026-05-22T00:00:00Z",
  "traceId": "trace-id",
  "details": null
}
```

Common statuses:

- `400` validation or malformed request
- `401` missing or invalid authentication
- `403` authenticated but not authorized
- `404` resource not found
- `410` expired, revoked, or exhausted share link
- `500` unexpected server error
