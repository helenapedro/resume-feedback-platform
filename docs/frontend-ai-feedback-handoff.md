# Frontend Implementation Instructions: AI Review and Progress UI

## Goal

Fix the resume detail AI review UI so it reflects the backend response accurately.

Current frontend problems visible in production:

- AI feedback can display Portuguese or mixed-language text from older generated records.
- `Strength Signals` and `Gaps to Close` are treated like fixed-size sections.
- The progress score card always shows `0/100`.
- Feedback text is too generic because the UI is only surfacing short labels and may be truncating or slicing richer backend content.

Backend changes are in place for new generations:

- New AI feedback is English-only.
- New feedback targets 4 to 5 strengths and 4 to 5 gaps.
- New progress analysis returns a meaningful version-to-version improvement score.
- The progress API now returns both `progressScore` and `score` for compatibility.

Existing stored records are not automatically rewritten. Users must regenerate a review to replace old Portuguese, short, or zero-score AI output.

## API Contracts

### Latest Feedback

Use this endpoint for the main feedback section:

`GET /api/resumes/{resumeId}/versions/{versionId}/ai-feedback`

Expected response shape:

```ts
type AiFeedback = {
  resumeId: string;
  resumeVersionId: string;
  jobId: string;
  feedbackVersion: number;
  mongoDocId: string;
  model: string;
  promptVersion: string | null;
  createdAt: string;
  summary: string;
  strengths: string[];
  improvements: string[];
};
```

Example:

```json
{
  "resumeId": "77caf16d-d735-48f9-8e6b-c81773673c08",
  "resumeVersionId": "current-version-id",
  "jobId": "job-id",
  "feedbackVersion": 2,
  "mongoDocId": "mongo-id",
  "model": "gemini-1.5-flash",
  "promptVersion": "v3",
  "createdAt": "2026-05-21T17:00:00Z",
  "summary": "This resume reads as a mid-level Analytics Engineer profile with credible SQL and data modeling evidence, but it needs stronger business impact and platform ownership proof to compete for senior roles.",
  "strengths": [
    "Skills: SQL, Python, and data modeling are directly aligned with analytics engineering roles and give recruiters a clear technical baseline.",
    "Experience: The resume shows hands-on work with ETL pipelines, which supports credibility for production data workflows.",
    "Projects: The analytics project examples help show applied problem solving rather than only listing tools.",
    "Clarity: The profile is easy to scan because the main data stack appears early."
  ],
  "improvements": [
    "Experience: Add the size, frequency, or business use of each pipeline so the reader can judge operational scope.",
    "Impact: Quantify dashboard, model, or data quality outcomes with metrics such as time saved, adoption, accuracy, or revenue influence.",
    "Seniority: Clarify whether the candidate owned architecture decisions, stakeholder tradeoffs, or production reliability.",
    "Projects: Replace generic project descriptions with problem, approach, stack, and measurable result."
  ]
}
```

### Progress Comparison

Use this endpoint for the version-to-version progress panel:

`GET /api/resumes/{resumeId}/versions/{versionId}/ai-progress`

Expected response shape:

```ts
type AiProgress = {
  resumeId: string;
  resumeVersionId: string;
  baselineResumeVersionId: string;
  jobId: string;
  progressVersion: number;
  mongoDocId: string;
  model: string;
  promptVersion: string | null;
  createdAt: string;
  summary: string;
  progressStatus: 'IMPROVED' | 'UNCHANGED' | 'DECLINED' | string;
  progressScore: number | null;
  score?: number | null;
  improvedAreas: string[];
  unchangedIssues: string[];
  newIssues: string[];
};
```

`progressScore` is the canonical score field. `score` is only a compatibility alias.

The score means: how much the current resume version improved compared with the previous version. It is not the overall resume quality score.

### Regenerate Review

Use this endpoint when the user clicks `Regenerate`:

`POST /api/resumes/{resumeId}/versions/{versionId}/ai-jobs/regenerate`

Do not send `language=PT` or any language query parameter. The backend now forces English-only output.

After regeneration:

1. Poll or refresh the latest AI job until it is `DONE` or `FAILED`.
2. Refetch `ai-feedback`.
3. Refetch `ai-progress` if the version has a previous version.

## Data Binding Rules

### Summary

Render `feedback.summary` exactly as returned. It should wrap naturally and should not be truncated to a single line.

If `feedback.summary` is missing:

- Show an empty state: `No AI summary is available for this version.`
- Do not invent fallback text.

### Strength Signals

Use the backend array directly:

```ts
const strengths = feedback?.strengths ?? [];
const strengthCount = strengths.length;
```

Render every item in `strengths`.

Do not:

- Hard-code `2`, `3`, or any other count.
- Use `.slice(0, 2)`.
- Collapse items into short generated labels.
- Replace backend text with frontend template text.

### Gaps to Close

Use the backend array directly:

```ts
const gaps = feedback?.improvements ?? [];
const gapCount = gaps.length;
```

Render every item in `improvements`.

Do not call this field `gaps` in API code unless it maps explicitly from `improvements`.

### Progress Score

Bind the score card like this:

```ts
const rawProgressScore = progress?.progressScore ?? progress?.score;
const hasProgressScore = rawProgressScore !== null && rawProgressScore !== undefined;
const progressScore = hasProgressScore
  ? Math.max(0, Math.min(100, Math.round(rawProgressScore)))
  : null;
```

Render:

```tsx
{progressScore === null ? 'Not scored' : `${progressScore}/100`}
```

Do not use `progress?.score ?? 0` or `progress?.progressScore ?? 0`. A missing score is not the same as a score of zero.

### Progress Status

Map backend statuses to UI labels:

```ts
const progressStatusLabel: Record<string, string> = {
  IMPROVED: 'Updated',
  UNCHANGED: 'No major change',
  DECLINED: 'Needs review',
};
```

If the backend returns an unknown status, display it as sentence-case text or `Unknown`.

### Progress Lists

Use the arrays directly:

```ts
const improvedAreas = progress?.improvedAreas ?? [];
const unchangedIssues = progress?.unchangedIssues ?? [];
const newIssues = progress?.newIssues ?? [];
```

Render every item in each list. If a list is empty, keep the current empty state:

- Improved areas: `No improvements detected for this comparison.`
- Still needs work: `No items in this category for the current comparison.`
- New issues: `No items in this category for the current comparison.`

## Loading, Empty, Error, and Legacy States

### Loading

During loading:

- Show skeletons or subtle loading placeholders.
- Do not show `0/100`.
- Do not show `0` counts unless the API has completed and returned an empty array.
- Do not show stale feedback for the wrong selected version.

Recommended guards:

```ts
const isFeedbackReady = !feedbackLoading && feedback;
const isProgressReady = !progressLoading && progress;
```

### No Feedback Yet

If `ai-feedback` returns 404 or the latest job is not `DONE`:

- Show the AI job status.
- Keep the feedback cards in a pending state.
- Do not render empty arrays as if the AI review finished.

Suggested copy:

`AI review is still being generated.`

### No Progress Yet

Progress only exists when a version has a previous version and baseline feedback exists.

If the selected version is version 1 or the progress endpoint returns 404:

- Hide the comparison score card.
- Show the existing message: `Progress comparison starts after you upload a second version.`

### Legacy AI Output

Old records may have:

- `promptVersion` missing, `v1`, or `v2`.
- Portuguese text.
- Two-item strengths or gaps.
- `progressScore: 0` even when status says improved.

Detect legacy feedback:

```ts
const isLegacyFeedback =
  !feedback?.promptVersion || feedback.promptVersion < 'v3';
```

For legacy feedback:

- Still render the data that exists.
- Show a small non-blocking notice near the regenerate button.
- Encourage regeneration.

Suggested copy:

`This review uses an older feedback format. Regenerate to get the latest English-only review.`

Do not try to translate old feedback in the frontend.

## Layout Requirements

The updated backend returns longer, more specific items. The UI must support this.

### Desktop

- Keep `Strength Signals` and `Gaps to Close` as two side-by-side panels when there is enough width.
- The panels should be allowed to grow vertically.
- Do not force equal fixed heights if content becomes clipped.
- Recommendation item boxes should wrap text naturally.

### Mobile

- Stack panels vertically.
- Keep readable spacing between list items.
- Avoid text overflow inside badges, count cards, and recommendation boxes.
- The score card should not overlap the progress summary.

### Text Handling

Use wrapping instead of truncation:

```css
white-space: normal;
overflow-wrap: anywhere;
line-height: 1.45;
```

Avoid ellipsis for feedback items. These are the core product value, not metadata labels.

## Suggested Component Logic

```tsx
function AiFeedbackPanel({ feedback, loading }: Props) {
  if (loading) return <AiFeedbackSkeleton />;
  if (!feedback) return <EmptyAiFeedback />;

  const strengths = feedback.strengths ?? [];
  const gaps = feedback.improvements ?? [];
  const isLegacy = !feedback.promptVersion || feedback.promptVersion < 'v3';

  return (
    <>
      {isLegacy && <LegacyReviewNotice />}

      <section>
        <h3>Overall Assessment</h3>
        <p>{feedback.summary || 'No AI summary is available for this version.'}</p>
      </section>

      <MetricCard label="Strength Signals" value={strengths.length} />
      <MetricCard label="Gaps to Close" value={gaps.length} />

      <FeedbackList title="Strength Signals" items={strengths} emptyText="No strength signals were returned." />
      <FeedbackList title="Gaps to Close" items={gaps} emptyText="No gaps were returned." />
    </>
  );
}
```

```tsx
function ProgressPanel({ progress, loading, hasPreviousVersion }: Props) {
  if (!hasPreviousVersion) {
    return <p>Progress comparison starts after you upload a second version.</p>;
  }

  if (loading) return <ProgressSkeleton />;
  if (!progress) return <p>No progress comparison is available yet.</p>;

  const rawScore = progress.progressScore ?? progress.score;
  const score = rawScore === null || rawScore === undefined
    ? null
    : Math.max(0, Math.min(100, Math.round(rawScore)));

  return (
    <section>
      <StatusBadge status={progress.progressStatus} />
      <MetricCard label="Score" value={score === null ? 'Not scored' : `${score}/100`} />
      <p>{progress.summary}</p>
      <ProgressList title="What Improved" items={progress.improvedAreas ?? []} />
      <ProgressList title="Still Needs Work" items={progress.unchangedIssues ?? []} />
      <ProgressList title="New Issues" items={progress.newIssues ?? []} />
    </section>
  );
}
```

## Acceptance Criteria

The implementation is done when all of the following are true:

- A new regenerated review displays English-only text across summary, strengths, gaps, and progress.
- `Strength Signals` count equals `feedback.strengths.length`.
- `Gaps to Close` count equals `feedback.improvements.length`.
- All returned strength and improvement items are visible.
- The UI handles 0, 1, 2, 4, 5, and 6 list items without layout breakage.
- The progress score card reads from `progress.progressScore` first, then `progress.score`.
- The progress score card does not show `0/100` while loading or when the score field is absent.
- Version 1 does not show a fake progress score.
- Legacy feedback still renders and shows a regenerate notice.
- Long feedback items wrap cleanly on desktop and mobile.

## QA Test Cases

1. New English review

Regenerate a review for any resume version. Verify:

- `promptVersion` is `v3`.
- Summary is English.
- Strengths are English.
- Improvements are English.
- Counts match array lengths.

2. Portuguese resume

Upload or regenerate a resume whose source text is Portuguese. Verify:

- The AI output is English.
- The UI does not display Portuguese phrases copied from the model output.
- Role and evidence names are translated or described naturally.

3. Variable item counts

Mock or test with payloads containing:

- 0 strengths, 0 improvements.
- 2 strengths, 2 improvements.
- 4 strengths, 4 improvements.
- 5 strengths, 5 improvements.
- 6 strengths, 6 improvements.

Verify layout remains usable in each case.

4. Progress score

Mock these payloads:

```json
{ "progressStatus": "IMPROVED", "progressScore": 75, "score": 75 }
```

Expected UI: `75/100`

```json
{ "progressStatus": "UNCHANGED", "progressScore": 0, "score": 0 }
```

Expected UI: `0/100`

```json
{ "progressStatus": "IMPROVED", "progressScore": null, "score": null }
```

Expected UI: `Not scored`

5. Loading state

Throttle the progress request. Verify the score card does not briefly flash `0/100`.

6. Version switching

Switch quickly between v1 and v2. Verify:

- v1 shows no progress comparison.
- v2 shows progress only for v2.
- Stale v2 score does not appear while v1 is selected.

## Known Backend Notes

- Backend stores AI feedback documents in MongoDB. Old documents remain old until regeneration.
- `promptVersion: v3` means the backend used the English-only, richer feedback prompt.
- Progress scoring is generated only for versions that have a previous version and baseline feedback.
- The frontend should not compute its own AI score from list counts.

