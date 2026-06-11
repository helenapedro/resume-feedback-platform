# Documentation Index

Detailed project documentation lives here.

## Recommended Reading Order

1. [Requirements](project-documentation/requirements.md) - product capabilities and system expectations
2. [Architecture](project-documentation/architecture.md) - service boundaries and data flow
3. [API Reference](project-documentation/API.md) - backend endpoint summary
4. [Operations](project-documentation/operations.md) - local setup, deployment, and troubleshooting

## Documents

### [Requirements](project-documentation/requirements.md)

- Functional capabilities and use cases
- Non-functional expectations
- Business rules and main flows

### [Architecture](project-documentation/architecture.md)

- Architectural style and component boundaries
- Data flow and integration strategy
- Persistence strategy

### [API Reference](project-documentation/API.md)

- Authentication, profile, resume, share-link, comment, and AI endpoints
- Request and response shape notes
- Error contract summary

### [Operations](project-documentation/operations.md)

- Local setup and runtime configuration
- Testing and deployment workflow
- Monitoring and troubleshooting checklist

## Module Docs

- [API Module](../resume-api/README.md)
- [Worker Module](../resume-worker/README.md)
- [Common Module](../common/README.md)

## Agents League Notes

- Language support: the platform accepts resumes in Portuguese and English and is designed for language-aware feedback.
- Development: GitHub Copilot inside VS Code assisted documentation, debugging, and safe implementation planning.
- AI provider abstraction: worker integrations use the `AiProviderClient` contract; `APP_AI_PROVIDER` lets each environment choose the provider that fits best, including Azure OpenAI as an optional Microsoft Foundry-compatible path.
- Submission package: see [Agents League Submission](agents-league-submission.md).
