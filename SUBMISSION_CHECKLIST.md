# Submission Checklist

## Product signal

- [x] Live app exists at `https://feedback.hmpedro.com`
- [x] Landing, auth, resume library, profile, and resume detail flows are present
- [x] Resume detail page shows PDF preview, AI feedback, comments, and share-link sections

## Challenge packaging

- [x] [README.md](README.md) gives a strong first impression
- [x] [JUDGES_GUIDE.md](JUDGES_GUIDE.md) gives evaluators a fast path
- [x] [DEMO.md](DEMO.md) uses the supported local `dev` profile
- [x] [docs/submission.md](docs/submission.md) explains product value and architecture
- [x] [docs/codex-build-story.md](docs/codex-build-story.md) explains how Codex contributed

## Execution credibility

- [x] Async AI pipeline is wired through API and worker, with optional Kafka support in code
- [x] Local development supports `LOCAL` storage instead of requiring S3
- [x] Share-link access behavior has integration coverage
- [x] AI job publication behavior has integration coverage
- [x] Worker processing has success and retry-path coverage

## Final polish

- [ ] Record a short demo video
- [ ] Add seeded sample resumes or a demo account path
- [ ] Re-run module tests and package build before final submission
- [ ] Verify all docs links once before submitting
