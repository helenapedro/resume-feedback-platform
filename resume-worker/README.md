# resume-worker

Asynchronous worker module for AI job processing.

For platform context and architecture, see the [root README](../README.md).

## Purpose

`resume-worker` processes AI jobs in the background, generates resume feedback through a configurable AI provider, stores outputs, generates version-to-version progress analysis when possible, and updates job state.

## Responsibilities

- Process pending AI jobs in the background
- Transition job lifecycle (`PENDING -> PROCESSING -> DONE/FAILED`)
- Generate AI feedback from prompt payloads
- Generate AI progress analysis for newer resume versions using the previous version plus prior AI feedback as baseline context
- Persist AI feedback documents in MongoDB
- Persist feedback/progress references and job state in MySQL
- Retry failed jobs using scheduled backoff policy

## Key Dependencies

- Spring Boot
- Spring Kafka (optional integration)
- Spring Data JPA (MySQL)
- Spring Data MongoDB
- Spring Boot Actuator

## Local Run

From repo root:

```bash
./mvnw -pl resume-worker spring-boot:run -Dspring-boot.run.profiles=dev
```

## Configuration (Important)

Core variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_MONGODB_URI`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` (only if Kafka is enabled)
- `KAFKA_PREFIX` (optional)

AI provider:

- `APP_AI_PROVIDER` selects the active provider for each environment. Packaged fallback default: `gemini`.
- `APP_AI_MODEL` sets the default model used by provider-specific config. Packaged fallback default: `gemini-1.5-flash`.
- `APP_AI_MAX_OUTPUT_TOKENS` sets the default generated JSON token cap. Default: `1800`.
- `APP_AI_TEMPERATURE` sets the default model temperature. Default: `0.2`.

Gemini:

- `GEMINI_API_KEY` (required for real AI responses)
- `app.ai-feedback.gemini.enabled` (default true in current config)
- `app.ai-feedback.gemini.model`
- `app.ai-feedback.gemini.temperature`
- `app.ai-feedback.gemini.max-output-tokens`
- `app.ai-feedback.max-resume-chars`
- `app.ai-feedback.progress-enabled`

OpenAI:

- `OPENAI_API_KEY` (required when `APP_AI_PROVIDER=openai`)
- `APP_AI_FEEDBACK_OPENAI_MODEL` overrides `APP_AI_MODEL` for OpenAI only. Default fallback: `gpt-4o-mini`.
- `APP_AI_FEEDBACK_OPENAI_MAX_OUTPUT_TOKENS` overrides `APP_AI_MAX_OUTPUT_TOKENS` for OpenAI only.
- `APP_AI_FEEDBACK_OPENAI_TEMPERATURE` overrides `APP_AI_TEMPERATURE` for OpenAI only.
- `APP_AI_FEEDBACK_OPENAI_BASE_URL` defaults to `https://api.openai.com`.

Azure OpenAI:

- `APP_AI_PROVIDER=azure-openai` selects the optional Azure OpenAI provider.
- `APP_AI_FEEDBACK_AZURE_OPENAI_ENABLED=true` enables the provider.
- `AZURE_OPENAI_API_KEY` is required when the provider is selected.
- `AZURE_OPENAI_ENDPOINT` is the Azure OpenAI resource endpoint, for example `https://your-resource.openai.azure.com`.
- `AZURE_OPENAI_DEPLOYMENT` is the Azure OpenAI deployment name used in the chat completions URL.
- `AZURE_OPENAI_API_VERSION` defaults to `2024-08-01-preview`.
- `APP_AI_FEEDBACK_AZURE_OPENAI_MAX_OUTPUT_TOKENS` overrides `APP_AI_MAX_OUTPUT_TOKENS` for Azure OpenAI only.
- `APP_AI_FEEDBACK_AZURE_OPENAI_TEMPERATURE` overrides `APP_AI_TEMPERATURE` for Azure OpenAI only.

Microsoft IQ / Foundry IQ grounding:

- `APP_AI_FEEDBACK_FOUNDRY_IQ_ENABLED=true` enables grounding context injection in feedback and progress prompts.
- `APP_AI_FEEDBACK_FOUNDRY_IQ_SOURCE` selects the knowledge source. Supported values: `local` and `azure-search`. Default: `local`.
- `APP_AI_FEEDBACK_FOUNDRY_IQ_MAX_CONTEXT_CHARS` limits retrieved grounding context. Default: `1800`.
- `AZURE_SEARCH_ENDPOINT` is required when `APP_AI_FEEDBACK_FOUNDRY_IQ_SOURCE=azure-search`.
- `AZURE_SEARCH_INDEX_NAME` is required when Azure AI Search grounding is enabled.
- `AZURE_SEARCH_API_KEY` is required when Azure AI Search grounding is enabled.
- `AZURE_SEARCH_API_VERSION` defaults to `2024-07-01`.
- `AZURE_SEARCH_QUERY_TYPE` defaults to `semantic`.
- `AZURE_SEARCH_SEMANTIC_CONFIGURATION` can be set when the index has semantic ranking configured.
- `AZURE_SEARCH_CONTENT_FIELD`, `AZURE_SEARCH_TITLE_FIELD`, and `AZURE_SEARCH_URL_FIELD` map index fields into cited grounding snippets.

Cost-control environment overrides:

- `APP_AI_FEEDBACK_MAX_RESUME_CHARS` limits resume text sent to the active AI provider. Default: `8000`. Progress analysis uses half of this value per resume version, with a minimum of `1500`, and sends head/tail excerpts instead of two full resume bodies.
- Feedback responses are intentionally compact: one short summary, 3 strengths, and 3 improvements. This keeps provider responses under output limits and reduces retry churn.
- `APP_AI_FEEDBACK_GEMINI_MAX_OUTPUT_TOKENS` overrides `APP_AI_MAX_OUTPUT_TOKENS` for Gemini only. Avoid setting this too low; truncated JSON causes failed jobs and can cost more through retries.
- `APP_AI_FEEDBACK_GEMINI_TEMPERATURE` overrides `APP_AI_TEMPERATURE` for Gemini only.
- `APP_AI_FEEDBACK_PROGRESS_ENABLED=false` disables version-to-version progress calls. This can roughly halve AI calls for second and later resume versions.
- `APP_AI_JOBS_RETRY_MAX_ATTEMPTS` caps failed-job retries. Default: `3`.

## Runtime Behavior

- If Kafka is enabled, topic: `${KAFKA_PREFIX}resume-ai-jobs` and consumer group: `${KAFKA_PREFIX}resume-worker`
- The scheduler polls `PENDING` jobs only when Kafka is disabled. When Kafka is enabled, it only retries due failed jobs.
- Failed jobs are retried on schedule until max attempts are reached.
- Progress analysis is skipped for the first version of a resume, or when the previous version has no stored baseline feedback yet.

## Tests

```bash
./mvnw -pl resume-worker test
```

## Notes

- The worker talks to AI providers through `AiProviderClient` and `AiProviderRegistry`. Provider-specific code maps model responses into platform-neutral feedback and progress records before MongoDB persistence.
- The platform supports Portuguese and English resumes; prompts and output handling are designed for language-aware feedback.
- Microsoft IQ / Foundry IQ grounding is implemented before provider execution. When enabled, the worker retrieves cited review knowledge through Azure AI Search or the packaged local knowledge source and injects it into prompts as rubric context.
- Azure OpenAI is available as an optional provider path: the adapter maps Azure OpenAI / Microsoft Foundry-compatible endpoints into the existing `AiProviderClient` contract so provider switching can be configured and validated through `APP_AI_PROVIDER`.
- Development was assisted by GitHub Copilot inside VS Code.
- Stored model values include the provider prefix, such as `gemini:gemini-1.5-flash`, so historical feedback remains auditable after provider changes.
- If the active provider is disabled or fails, the AI job is marked failed and retried according to the configured retry policy.
- Monitor logs for worker polling, processing, and job status transitions.
- Progress-analysis failures do not fail the primary feedback job; the worker logs and skips that secondary artifact.
