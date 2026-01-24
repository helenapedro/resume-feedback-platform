# Integração Frontend com o Backend (resume-api, resume-worker, common)

Este documento detalha como o frontend deve integrar com as APIs existentes, baseado no comportamento atual do `resume-api`, `resume-worker` e módulo `common`.

## Visão geral
- Autenticação via JWT (Bearer token) para endpoints privados em `/api/**`, exceto `/api/auth/**` e `/api/share/**`.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/security/SecurityConfig.java†L20-L38】
- Operações de resume (upload, versões, download) e comentários para o dono exigem JWT; endpoints públicos usam token de share-link.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/web/ResumeController.java†L23-L95】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/comment/web/CommentOwnerController.java†L17-L45】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/comment/web/CommentPublicController.java†L17-L52】
- AI feedback é produzido pelo `resume-worker` e persistido em MongoDB, referenciado por MySQL; o frontend só consome via endpoints do `resume-api`.【F:feedback/resume-worker/src/main/java/com/pedro/resumeworker/ai/service/AiJobProcessor.java†L26-L128】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/ai/service/AiFeedbackService.java†L24-L77】

## Autenticação
### Register
`POST /api/auth/register`

**Request**
```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

**Response**
```json
{
  "accessToken": "<jwt>"
}
```
- Email e password não podem ser vazios; email duplicado retorna erro de validação/illegal argument.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/security/AuthController.java†L21-L55】

### Login
`POST /api/auth/login`

**Request**
```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

**Response**
```json
{
  "accessToken": "<jwt>"
}
```
- Token deve ser enviado em `Authorization: Bearer <jwt>`; caso inválido/ausente retorna 401 (filtro JWT).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/security/JwtAuthFilter.java†L25-L67】

## Resumes (JWT obrigatório)
Base: `/api/resumes`

### Listar resumes
`GET /api/resumes`

**Response** (array de `ResumeSummaryDTO`):
```json
[
  {
    "id": "uuid",
    "title": "My Resume",
    "currentVersionId": "uuid",
    "createdAt": "2024-01-01T00:00:00Z"
  }
]
```
Campos em `ResumeSummaryDTO`: `id`, `title`, `currentVersionId`, `createdAt`.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/dto/ResumeSummaryDTO.java†L7-L14】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/web/ResumeController.java†L25-L35】

### Detalhar resume
`GET /api/resumes/{id}`

**Response**
```json
{
  "resume": { "id": "uuid", "title": "My Resume", "currentVersionId": "uuid", "createdAt": "..." },
  "versions": [
    {
      "id": "uuid",
      "versionNumber": 1,
      "originalFilename": "resume.pdf",
      "contentType": "application/pdf",
      "fileSizeBytes": 12345,
      "createdById": "uuid",
      "createdAt": "..."
    }
  ]
}
```
`versions` são ordenadas por número de versão desc (mais recente primeiro).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/web/ResumeController.java†L37-L56】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/dto/ResumeVersionDTO.java†L7-L15】

### Criar resume (upload)
`POST /api/resumes` (multipart/form-data)

- **Part obrigatório:** `file`
- **Campo opcional:** `title`

Backend cria a versão 1 e dispara job de AI automaticamente.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/web/ResumeController.java†L58-L70】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/service/ResumeService.java†L46-L79】

### Adicionar nova versão
`POST /api/resumes/{id}/versions` (multipart/form-data)

- **Part obrigatório:** `file`

Backend incrementa o `versionNumber`, atualiza `currentVersion` e dispara novo job de AI.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/web/ResumeController.java†L72-L84】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/service/ResumeService.java†L81-L106】

### Download de versão (owner)
`GET /api/resumes/{resumeId}/versions/{versionId}/download`

- Pode responder **302** com `Location` (presigned S3) ou **200** com stream binário direto. O frontend deve seguir redirects automaticamente ou tratar `Location` manualmente.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/web/ResumeController.java†L86-L110】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/resume/service/ResumeStorageService.java†L31-L73】

## Share Links
### Permissões
- `VIEW` (apenas visualizar/download)
- `COMMENT` (visualizar + comentar)

Definição na entidade `ShareLink.Permission`.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/domain/ShareLink.java†L16-L19】

### Criar share link (owner)
`POST /api/resumes/{resumeId}/share-links`

**Request**
```json
{
  "permission": "VIEW",
  "expiresAt": "2024-12-31T23:59:59Z",
  "maxUses": 100
}
```

**Response**
```json
{
  "id": "uuid",
  "token": "plain-token",
  "permission": "VIEW",
  "expiresAt": "2024-12-31T23:59:59Z",
  "maxUses": 100
}
```
O token plaintext só retorna na criação; depois é armazenado apenas como hash.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/service/ShareLinkService.java†L32-L72】

### Listar share links (owner)
`GET /api/resumes/{resumeId}/share-links`

**Response** (array de `ShareLinkDTO`)
```json
[
  {
    "id": "uuid",
    "permission": "VIEW",
    "expiresAt": "...",
    "revokedAt": null,
    "maxUses": 100,
    "useCount": 2,
    "createdAt": "...",
    "createdBy": "uuid"
  }
]
```
Campos em `ShareLinkDTO`.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/dto/ShareLinkDTO.java†L8-L19】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/web/ShareLinkOwnerController.java†L39-L44】

### Revogar share link (owner)
`POST /api/resumes/{resumeId}/share-links/{linkId}/revoke`

Sem payload. Se `linkId` não estiver no resume do usuário, retorna 403.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/web/ShareLinkOwnerController.java†L46-L50】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/service/ShareLinkService.java†L96-L112】

### Abrir share link (público)
`GET /api/share/{token}`

**Response** (`ShareLinkPublicDTO`)
```json
{
  "resumeId": "uuid",
  "currentVersionId": "uuid",
  "permission": "COMMENT",
  "expiresAt": "..."
}
```
Cada chamada **incrementa useCount** do share link (conta como uso).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/web/ShareLinkPublicController.java†L18-L35】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/service/ShareLinkService.java†L75-L94】

### Download público (token)
`GET /api/share/{token}/download`

- Valida token e versão atual.
- Retorna 302 (S3) ou 200 com arquivo.
- Se resume não tiver `currentVersion`, retorna erro `NO_CURRENT_VERSION` (400).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/sharelink/web/ShareLinkPublicDownloadController.java†L27-L66】

### Rate limit em `/api/share/**`
- O filtro de rate limit usa IP (`X-Forwarded-For`, `X-Real-IP` ou remoto) e responde **429** com header `Retry-After` quando excede limite configurado.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/security/ShareRateLimitFilter.java†L25-L70】

## Comentários
### Estrutura de comentário
`CommentDTO` inclui:
- `authorUserId` (null para guest)
- `authorLabel` ("Owner" ou label do guest)
- `anchorRef` (referência do trecho do currículo)
- `parentCommentId` (null se for top-level)
【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/comment/dto/CommentDTO.java†L7-L17】

### Criar comentário (owner)
`POST /api/resumes/{resumeId}/versions/{versionId}/comments`

**Request**
```json
{
  "body": "Texto do comentário",
  "anchorRef": "page-1:line-5",
  "parentCommentId": null,
  "guestLabel": null
}
```
`guestLabel` é ignorado para owner. Comentários são salvos com `authorLabel = "Owner"`.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/comment/service/CommentService.java†L33-L76】

### Criar comentário (público)
`POST /api/share/{token}/comments`

**Request**
```json
{
  "body": "Sugestão",
  "anchorRef": "page-1:line-10",
  "parentCommentId": null,
  "guestLabel": "Maria"
}
```
- Só permitido quando o share link tem `permission = COMMENT`. Caso contrário retorna 403.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/comment/service/CommentService.java†L99-L151】

### Listar comentários (owner/público)
- Owner: `GET /api/resumes/{resumeId}/versions/{versionId}/comments`
- Público: `GET /api/share/{token}/comments`

Ordenados por `createdAt` asc (threading manual).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/comment/service/CommentService.java†L78-L97】

## AI Jobs (owner)
### Obter job mais recente
`GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest`

**Response** (`AiJobDTO`)
```json
{
  "id": "uuid",
  "resumeVersionId": "uuid",
  "status": "PENDING",
  "attemptCount": 0,
  "createdAt": "...",
  "updatedAt": "...",
  "startedAt": "...",
  "finishedAt": "...",
  "errorCode": null,
  "errorDetail": null,
  "nextRetryAt": null
}
```
Status possíveis: `PENDING`, `PROCESSING`, `DONE`, `FAILED`.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/ai/dto/AiJobDTO.java†L7-L19】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/ai/domain/AiJob.java†L20-L63】

### Reprocessar
`POST /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/regenerate`

- Cria novo job com idempotency key única (nova tentativa).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/ai/service/AiJobService.java†L51-L71】

## AI Feedback (owner)
`GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback`

**Response** (`AiFeedbackDTO`)
```json
{
  "resumeId": "uuid",
  "resumeVersionId": "uuid",
  "jobId": "uuid",
  "feedbackVersion": 1,
  "mongoDocId": "...",
  "model": "gpt-4o-mini",
  "promptVersion": "v1",
  "createdAt": "...",
  "summary": "...",
  "strengths": ["..."],
  "improvements": ["..."]
}
```
O feedback é criado no worker a partir da mensagem Kafka (`AiJobRequestedMessage`) e persistido em MongoDB; o API apenas lê o último disponível.【F:feedback/common/src/main/java/com/pedro/common/ai/AiJobRequestedMessage.java†L5-L12】【F:feedback/resume-worker/src/main/java/com/pedro/resumeworker/ai/service/AiFeedbackFactory.java†L15-L52】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/ai/service/AiFeedbackService.java†L47-L77】

## Erros e contratos
### Envelope padrão de erro
```json
{
  "code": "ERROR_CODE",
  "message": "...",
  "status": 400,
  "path": "/api/...",
  "timestamp": "...",
  "traceId": "...",
  "details": { "fieldErrors": { "body": "must not be blank" } }
}
```
Estrutura definida em `ApiErrorResponse` e usada pelo handler global.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/api/error/ApiErrorResponse.java†L5-L23】【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/api/error/GlobalExceptionHandler.java†L24-L212】

### Códigos relevantes
- `UNAUTHENTICATED`, `FORBIDDEN` (auth)
- `RESUME_NOT_FOUND`, `VERSION_NOT_FOUND`, `AI_JOB_NOT_FOUND`
- `VALIDATION_ERROR`, `INVALID_REQUEST`, `FILE_REQUIRED`
- `SHARE_LINK_NOT_FOUND`, `SHARE_LINK_GONE`
【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/api/error/ErrorCode.java†L3-L32】

### Casos importantes para UI
- **401** se JWT ausente/inválido (filtro responde sem body).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/security/JwtAuthFilter.java†L25-L67】
- **403** para operações sem permissão (ex: comentar com share link `VIEW`).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/comment/service/CommentService.java†L120-L151】
- **404** para token inválido/não encontrado (share link).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/api/error/GlobalExceptionHandler.java†L41-L65】
- **410** para share link expirado/revogado/exaurido (maxUses).【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/api/error/GlobalExceptionHandler.java†L67-L92】
- **429** rate limit em `/api/share/**` com `Retry-After`.【F:feedback/resume-api/src/main/java/com/pedro/resumeapi/security/ShareRateLimitFilter.java†L42-L70】

## Fluxos recomendados para o frontend
### Upload + feedback
1. `POST /api/resumes` com arquivo.
2. Chamar `GET /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/latest` até `status = DONE`.
3. Buscar `GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback` para apresentar insights.

### Compartilhamento
1. Criar share link com `permission = VIEW` ou `COMMENT`.
2. Exibir/compartilhar URL pública: `/api/share/{token}` e download `/api/share/{token}/download`.
3. Para comentários públicos, usar `/api/share/{token}/comments` (somente se `permission = COMMENT`).

### Comentários aninhados
- Use `parentCommentId` para encadear respostas.
- `anchorRef` é livre e pode ser definido pelo UI (ex: seleção de texto, página/linha).

---

Se precisar de exemplos adicionais ou mapeamento para telas específicas do frontend, adicione novas seções neste arquivo.
