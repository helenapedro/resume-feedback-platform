# Auth
/api/auth/login [POST]
```json
{
"email": "helenaexplora@hmpedro.com",
"password": "Explora@26"
}
```
[Response]
```json
{
    "accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJpc3MiOiJyZXN1bWUtYXBpIiwic3ViIjoiYTBiZjA4Y2QtNzdiZS00YThmLWJkMDMtNjViZmI0NjMwNTkyIiwiZW1haWwiOiJoZWxlbmFleHBsb3JhQGhtcGVkcm8uY29tIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3Njg3OTg0OTIsImV4cCI6MTc2ODgwNTY5Mn0.Rp21E049ZVcy6SVramnI8EDoy_P1zB7e1In2qPRt4R08Ck0meHsGGD64CZ4L8V4y"
}
```

/api/auth/register [POST]
```json
{
  "email": "",
  "password": ""
}
```
[RESPONSE]
```json
{
    "accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJpc3MiOiJyZXN1bWUtYXBpIiwic3ViIjoiOTBiY2RlNzctY2RkNy00MGM2LWIxYjgtZmY0Y2JlMmI2YjNjIiwiZW1haWwiOiJwZWRyb0Bsb2NhbC5kZXYiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTc2ODc5ODgzMywiZXhwIjoxNzY4ODA2MDMzfQ.tMEW3ZEF8ivunDqSA2bNxkFGUJVhQB2X-bVn4y6VT6NlqFI1Pd0ANkiZp_ZFdxXO"
}
```

# RESUME
[GET] http://localhost:8080/api/resumes/{{resumeId}}/versions/{{versionId}}/ai-jobs/latest (auth)
{
"code": "AI_JOB_NOT_FOUND",
"message": "AI job not found",
"status": 404,
"path": "/api/resumes/100d7389-20b8-4b20-9871-f4cd7003e34e/versions/2194657b-f0bc-410b-8115-85b73ec4510e/ai-jobs/latest",
"timestamp": "2026-01-23T05:13:48.371057200Z",
"traceId": "70872d79-eb93-4c83-9dea-90f939e55c9f",
"details": null
}

[POST] http://localhost:8080/api/resumes/{{resumeId}}/versions/{{versionId}}/ai-jobs/regenerate

[GET] http://localhost:8080/api/resumes
```json
[
  {
    "id": "2ced546a-2246-49dd-9392-ff7821e5749e",
    "title": "Test",
    "currentVersionId": "c21851c6-84a3-4f48-b047-2d90f7320523",
    "createdAt": "2026-01-21T02:58:39.788137Z"
  },
  {
    "id": "3f795379-1d2a-45d5-b48f-6e719c438904",
    "title": "Curriculo Backend",
    "currentVersionId": "d6f6eca1-71c8-4e17-b794-a0759a8ffec5",
    "createdAt": "2026-01-18T21:43:40.988020Z"
  }
]
```
[POST] http://localhost:8080/api/resumes
BODY
    form-data
    | key   |      |    value      |
    | title | text | Backend       |
    | file  | file | MyResume.pdf  |
500
```json
{
    "code": "INTERNAL_ERROR",
    "message": "Unexpected error",
    "status": 500,
    "path": "/api/resumes",
    "timestamp": "2026-01-21T16:53:35.651330300Z",
    "traceId": "f3bf02af-6e74-4d5b-ad1c-92a68076f2eb",
    "details": null
}
```

200
```json
{
    "id": "100d7389-20b8-4b20-9871-f4cd7003e34e",
    "title": "Data",
    "currentVersionId": "d97dad67-63b6-495e-b10d-8d0cb77ed8fd",
    "createdAt": "2026-01-21T16:54:58.181000200Z"
}
```

[GET] http://localhost:8080/api/resumes/{{id}}
```json
{
    "resume": {
        "id": "3f795379-1d2a-45d5-b48f-6e719c438904",
        "title": "Curriculo Backend",
        "currentVersionId": "d6f6eca1-71c8-4e17-b794-a0759a8ffec5",
        "createdAt": "2026-01-18T21:43:40.988020Z"
    },
    "versions": [
        {
            "id": "d6f6eca1-71c8-4e17-b794-a0759a8ffec5",
            "versionNumber": 3,
            "originalFilename": "Resume-Pedro-HelenaMbeua.pdf",
            "contentType": "application/pdf",
            "fileSizeBytes": 216512,
            "createdById": "a0bf08cd-77be-4a8f-bd03-65bfb4630592",
            "createdAt": "2026-01-20T04:58:51.106925Z"
        },
        {
            "id": "8a2568bd-20c2-4319-83fb-e4bf684cfdf3",
            "versionNumber": 2,
            "originalFilename": "Helena-Pedro-Resume.pdf",
            "contentType": "application/pdf",
            "fileSizeBytes": 221561,
            "createdById": "a0bf08cd-77be-4a8f-bd03-65bfb4630592",
            "createdAt": "2026-01-19T01:11:50.262142Z"
        },
        {
            "id": "71b3576b-09c4-4049-8059-9a84efc0c807",
            "versionNumber": 1,
            "originalFilename": "Resume-Pedro-HelenaMbeua.pdf",
            "contentType": "application/pdf",
            "fileSizeBytes": 219889,
            "createdById": "a0bf08cd-77be-4a8f-bd03-65bfb4630592",
            "createdAt": "2026-01-18T21:43:41.008011Z"
        }
    ]
}
```
[POST] http://localhost:8080/api/resumes/{{id}}/versions (auth)
BODY
    form-data
| key   |      |    value      |
| file  | file | MyResume.pdf  |

500
```json
{
    "code": "INTERNAL_ERROR",
    "message": "Unexpected error",
    "status": 500,
    "path": "/api/resumes/100d7389-20b8-4b20-9871-f4cd7003e34e/versions",
    "timestamp": "2026-01-21T16:58:46.651372900Z",
    "traceId": "637f2d39-32a1-4c7f-921d-d61a7c0ad3eb",
    "details": null
}
```

200
```json
{
    "id": "2194657b-f0bc-410b-8115-85b73ec4510e",
    "versionNumber": 2,
    "originalFilename": "Resume-Pedro-HelenaMbeua.pdf",
    "contentType": "application/pdf",
    "fileSizeBytes": 215435,
    "createdById": "a0bf08cd-77be-4a8f-bd03-65bfb4630592",
    "createdAt": "2026-01-21T17:02:07.820995500Z"
}
```

[GET] http://localhost:8080/api/resumes/{{resumeId}}/versions/{{versionId}}/download

- It shows the actual resume in postman with status 200 ok

[GET] http://localhost:8080/api/resumes/{{resumeId}}/share-links
```json
[
    {
        "id": "13d63ba9-6f30-411d-934a-cc6c0a9b5656",
        "permission": "VIEW",
        "expiresAt": null,
        "revokedAt": null,
        "maxUses": 4,
        "useCount": 2,
        "createdAt": "2026-01-21T04:54:21Z",
        "createdBy": "a0bf08cd-77be-4a8f-bd03-65bfb4630592"
    },
    {
        "id": "c3385175-fa9a-4050-8de2-62006e2e3b4c",
        "permission": "VIEW",
        "expiresAt": null,
        "revokedAt": null,
        "maxUses": 4,
        "useCount": 4,
        "createdAt": "2026-01-21T03:06:52Z",
        "createdBy": "a0bf08cd-77be-4a8f-bd03-65bfb4630592"
    },
    {
        "id": "18ea8d90-f2c5-4d83-a0ad-d221da95b0d1",
        "permission": "VIEW",
        "expiresAt": "2026-01-21T00:00:00Z",
        "revokedAt": "2026-01-21T03:11:19Z",
        "maxUses": null,
        "useCount": 0,
        "createdAt": "2026-01-19T04:41:38Z",
        "createdBy": "a0bf08cd-77be-4a8f-bd03-65bfb4630592"
    },
    {
        "id": "e5a68311-3c5c-4037-bc1c-95e87fec8b07",
        "permission": "VIEW",
        "expiresAt": "2026-01-21T00:00:00Z",
        "revokedAt": "2026-01-20T04:57:46Z",
        "maxUses": null,
        "useCount": 0,
        "createdAt": "2026-01-19T04:40:29Z",
        "createdBy": "a0bf08cd-77be-4a8f-bd03-65bfb4630592"
    }
]
```
[POST] http://localhost:8080/api/resumes/{{resumeId}}/share-links (auth)
```json
{
  "permission": "VIEW",
  "maxUses": 4
}
```
200
```json
{
    "id": "5c94eb90-76b0-4fcc-8847-68021bbac357",
    "token": "1957a20b-3e04-4e57-8894-7b0e0c795c36-3926ec86-3bcf-4ed7-8720-b26d97e50db3",
    "permission": "COMMENT",
    "expiresAt": null,
    "maxUses": 4
}
```
[POST] http://localhost:8080/api/resumes/{{resumeId}}/share-links/{{linkId}}/revoke (auth)
 - 200 ok

[GET] http://localhost:8080/api/resumes/{{resumeId}}/versions/{{versionId}}/comments
200
```json
[
    {
        "id": "9c0c455e-b479-435f-b5d4-071a2b467d0f",
        "resumeVersionId": "71b3576b-09c4-4049-8059-9a84efc0c807",
        "authorUserId": "a0bf08cd-77be-4a8f-bd03-65bfb4630592",
        "authorLabel": "Owner",
        "body": "Commenting my own resume",
        "anchorRef": null,
        "parentCommentId": null,
        "createdAt": "2026-01-21T03:14:36Z",
        "updatedAt": null
    }
]
```
Criar comentário public (guest)

[POST] http://localhost:8080/api/resumes/{{resumeId}}/versions/{{versionId}}/comments (auth)
Content-Type: application/json
```json
{
  "body": "Comentário do guest",
  "anchorRef": "page=1:line=10",
  "guestLabel": "Recruiter",
  "parentCommentId": null
}
```
200
```json
{
    "id": "080d84f1-288d-4ca3-b5c2-6e22b15fac05",
    "resumeVersionId": "d97dad67-63b6-495e-b10d-8d0cb77ed8fd",
    "authorUserId": "a0bf08cd-77be-4a8f-bd03-65bfb4630592",
    "authorLabel": "Owner",
    "body": "Comentário do guest",
    "anchorRef": "page=1:line=10",
    "parentCommentId": null,
    "createdAt": "2026-01-21T17:27:52.473650Z",
    "updatedAt": null
}
```

```json
{
  "body": "test Commenting my own resume"
}
```
200
```json
{
    "id": "dc861832-2c88-41c6-a599-2e234040f928",
    "resumeVersionId": "d97dad67-63b6-495e-b10d-8d0cb77ed8fd",
    "authorUserId": "a0bf08cd-77be-4a8f-bd03-65bfb4630592",
    "authorLabel": "Owner",
    "body": "test Commenting my own resume",
    "anchorRef": null,
    "parentCommentId": null,
    "createdAt": "2026-01-21T17:22:40.253889300Z",
    "updatedAt": null
}
```

# SHARE
[GET] http://localhost:8080/api/share/{{token}} (auth)
example: 
[GET] http://localhost:8080/api/share/1957a20b-3e04-4e57-8894-7b0e0c795c36-3926ec86-3bcf-4ed7-8720-b26d97e50db3

200
```json
{
    "resumeId": "100d7389-20b8-4b20-9871-f4cd7003e34e",
    "currentVersionId": "2194657b-f0bc-410b-8115-85b73ec4510e",
    "permission": "COMMENT",
    "expiresAt": null
}
```

410
```json
{
    "code": "SHARE_LINK_GONE",
    "message": "Share link usage limit reached",
    "status": 410,
    "path": "/api/share/623854c2-f7d7-4881-ac8c-560d86c07760-b1ee09c9-d4f5-4731-9fb7-1d5cbf4584db",
    "timestamp": "2026-01-21T16:33:53.815009800Z",
    "traceId": "884c4893-7058-499a-a0e8-bc645f8c8868",
    "details": null
}
```

[GET] http://localhost:8080/api/share/{{token}}/download
200
- It shows the actual resume

410
```json
{
    "code": "SHARE_LINK_GONE",
    "message": "Share link usage limit reached",
    "status": 410,
    "path": "/api/share/623854c2-f7d7-4881-ac8c-560d86c07760-b1ee09c9-d4f5-4731-9fb7-1d5cbf4584db/download",
    "timestamp": "2026-01-21T16:36:43.123495200Z",
    "traceId": "d1a81c7a-687d-423f-ae65-acaceb9959cd",
    "details": null
}
```
[GET] http://localhost:8080/api/share/{{token}}/comments
410
```json
{
    "code": "SHARE_LINK_GONE",
    "message": "Share link usage limit reached",
    "status": 410,
    "path": "/api/share/623854c2-f7d7-4881-ac8c-560d86c07760-b1ee09c9-d4f5-4731-9fb7-1d5cbf4584db/comments",
    "timestamp": "2026-01-21T16:41:30.106892600Z",
    "traceId": "422fc704-c219-4a43-bc48-4a064a76ae52",
    "details": null
}
```
200
```json
[]
```

200
```json

```


[GET] http://localhost:8080/api/ping
```raw
pong (users=2)
```




