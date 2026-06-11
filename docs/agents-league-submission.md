# Agents League Submission

## Project

**Resume Feedback Platform** is a production-style creative app for resume iteration. It helps candidates upload a resume, receive structured AI feedback, compare progress across versions, and share controlled review links with mentors, recruiters, or peers.

The project is aimed at the **Creative Apps** track because it combines a user-facing product experience with AI-assisted development in GitHub Copilot and VS Code.

## Problem

The idea for Resume Feedback Platform came from conversations with talented, qualified people who asked for help planning their careers. When I looked at their resumes, I saw a painful gap: the documents did not communicate the real professional value those people already had.

Many companies are genuinely looking for candidates like them. The candidate may have experience, technical ability, potential, and a strong desire to grow. But a recruiter cannot guess that from context they do not have. The resume is often the first entry point, and writing a strong resume is its own skill.

Over time, I saw smart, capable people miss opportunities not because they lacked talent, but because their resumes did not present their professional journey clearly, structurally, and strategically. That problem is even sharper in a competitive, increasingly digital job market.

Resume Feedback Platform was created to reduce that communication barrier. The goal is not to replace a person's effort, experience, or judgment. The goal is to help people communicate what they already know how to do more clearly, and to make their talent, knowledge, and potential more visible through a continuous resume improvement process.

## Solution

Resume Feedback Platform turns resume review into a version-aware workflow:

1. A candidate uploads a resume PDF.
2. The platform creates an asynchronous AI review job.
3. A worker extracts resume text and generates structured feedback.
4. The candidate uploads new versions over time.
5. The platform compares the current version with the previous version and explains progress, unchanged issues, and new risks.
6. The candidate can share controlled review links for external feedback.

The product supports both Portuguese-speaking and English-speaking users. Candidates can submit resumes in Portuguese or English, while the AI workflow is designed to keep feedback useful for the user's language context and product workflow.

## Creative App Value

The creative part of the app is not only generating text. The app creates a structured improvement experience:

- Version-aware resume intelligence instead of one-off feedback.
- Progress analysis that explains whether a new version improved.
- Human-in-the-loop collaboration through comments and share links.
- A workflow that supports real job seekers, not only demo prompts.

## GitHub Copilot Usage

GitHub Copilot and VS Code were used as part of the development workflow. Copilot helped accelerate:

- Understanding and navigating the multi-module Spring Boot backend.
- Drafting and refining frontend/backend integration changes.
- Improving documentation and demo instructions.
- Debugging local development and Maven module issues.
- Reasoning through safer implementation boundaries for production code.
- Identifying how to extend the existing AI provider abstraction without rewriting the worker pipeline.

The project is not presented as fully AI-generated. It is presented as an example of AI-assisted software engineering where Copilot accelerated a real product workflow while human review guided architecture, safety, and product decisions.

## Microsoft IQ / Foundry Integration

The backend integrates a Microsoft IQ / Foundry IQ grounding layer in the worker prompt pipeline. When `APP_AI_FEEDBACK_FOUNDRY_IQ_ENABLED=true`, the worker retrieves cited resume-review knowledge and injects it into feedback and progress-analysis prompts as grounding context. The implementation supports Azure AI Search semantic retrieval, using the documented search endpoint pattern for indexed knowledge sources, and also includes a local knowledge source for safe demo environments.

Model access remains isolated behind the worker-side `AiProviderClient` abstraction. `APP_AI_PROVIDER` lets each environment select the provider that best fits its needs. Azure OpenAI is available as an optional provider path for Microsoft Foundry-compatible deployments without changing the existing AI job pipeline.

Low-risk implementation:

- `FoundryIqGroundingProvider` enriches prompts before the selected model provider is called.
- `APP_AI_FEEDBACK_FOUNDRY_IQ_ENABLED=false` keeps production behavior unchanged by default.
- `APP_AI_FEEDBACK_FOUNDRY_IQ_SOURCE=azure-search` enables Azure AI Search-backed retrieval when endpoint, index, and key are configured.
- `APP_AI_FEEDBACK_FOUNDRY_IQ_SOURCE=local` uses the packaged cited knowledge source for demo and offline environments.
- The grounded prompt still instructs the model not to invent resume evidence; retrieved knowledge is used only as rubric context.
- Azure OpenAI remains independently selectable with `APP_AI_PROVIDER=azure-openai`, `APP_AI_FEEDBACK_AZURE_OPENAI_ENABLED=true`, and Azure OpenAI credentials.

This aligns with the challenge's Foundry IQ direction: agentic knowledge retrieval, grounded context, and cited knowledge sources without destabilizing production. The version-comparison workflow is especially well suited for grounded reasoning because it uses:

- the previous resume version,
- the current resume version,
- prior AI feedback,
- Microsoft IQ / Foundry IQ grounding context when enabled,
- structured progress categories,
- and an explicit improvement score.

## Architecture Summary

The backend is a multi-module Spring Boot system:

- `resume-api`: authentication, resume upload, versioning, share links, comments, AI job orchestration, and read APIs.
- `resume-worker`: asynchronous AI processing, resume text extraction, feedback generation, progress analysis, retry behavior, and artifact persistence.
- `common`: shared contracts for AI jobs and MongoDB AI documents.

Persistence and infrastructure:

- MySQL stores users, resumes, versions, AI jobs, references, comments, share links, and access audit records.
- MongoDB stores AI feedback and progress documents.
- S3 or local storage stores uploaded resume files.
- Kafka is supported as an optional event-driven path.
- The hosted low-cost deployment can process jobs through scheduled polling.

## Demo Flow

The two-minute demo should show:

1. Open the hosted app.
2. Register or log in.
3. Upload a resume PDF.
4. Wait for the AI job to complete.
5. Show the AI feedback: summary, strengths, and improvements.
6. Upload a revised version.
7. Show progress comparison: improved areas, unchanged issues, new issues, and score.
8. Create a share link and show the collaboration/review workflow.
9. Briefly show the code/documentation in VS Code and mention GitHub Copilot assisted the development process.

## Security Notes

Before submitting a public repository:

- Do not commit `.env` files or credentials.
- Do not include API keys, database URLs, JWT secrets, S3 credentials, OAuth secrets, or personal data.
- Use demo resumes only.
- Keep production credentials in hosting provider environment variables.
- Review commit history and repository settings before making the repo public.

## Submission Checklist

- [ ] Innovation Studio profile activated.
- [ ] Project created and linked to the Creative Apps challenge.
- [ ] Public backend repository available.
- [ ] Public frontend repository available.
- [ ] README updated for Portuguese and English resume support.
- [ ] GitHub Copilot usage documented.
- [ ] Microsoft IQ / Foundry grounding integration documented.
- [ ] Architecture diagram included.
- [ ] Demo video recorded, 2 minutes max.
- [ ] Demo video uploaded to YouTube or Vimeo as a public link.
- [ ] Project description explains problem, solution, AI value, and technologies.
- [ ] Setup and usage instructions are clear.
- [ ] Repository checked for secrets and private data.
