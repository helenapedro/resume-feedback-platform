# Google Gemini (LLM) Integration Guide

This guide describes the step-by-step process to enable **Google Gemini** in the `resume-worker` and to evolve the pipeline with real resume content.

---

## 1) Prerequisites

1. **Google Cloud account** with the **Generative Language API** enabled.
2. **API Key** (do not use user credentials in production).

> Official reference: https://ai.google.dev/

---

## 2) Generate the API Key

1. Access the Google Cloud console.
2. Go to **APIs & Services → Credentials**.
3. Click **Create Credentials → API Key**.
4. Restrict the key (optional, but recommended) to the **Generative Language** API.

---

## 3) Configure environment variables

In the **resume-worker** environment:

```bash
export GEMINI_API_KEY="your_api_key"
```

In Docker/Kubernetes, add the variable to the container `env` or to Secret Manager.

---

## 4) Enable Gemini in the worker

In `resume-worker`, Gemini is already prepared, but disabled by default.

### Development
Edit `feedback/resume-worker/src/main/resources/application.yml`:

```yaml
app:
  ai-feedback:
    gemini:
      enabled: true
      api-key: ${GEMINI_API_KEY:}
```

### Production
`application-prod.yml` already enables Gemini if `GEMINI_API_KEY` is present:

```yaml
app:
  ai-feedback:
    gemini:
      enabled: true
      api-key: ${GEMINI_API_KEY:}
```

---

## 5) Adjust model and parameters

Parameters are configurable via YAML:

```yaml
app:
  ai-feedback:
    gemini:
      base-url: https://generativelanguage.googleapis.com
      model: gemini-1.5-flash
      temperature: 0.4
      max-output-tokens: 600
```

---

## 6) Send real resume content (essential step)

Today the worker does not receive the resume text, only IDs. To generate real feedback:

### Option A — Fetch content via internal API
1. Create an **internal** endpoint in `resume-api` that:
   - Receives `resumeVersionId`
   - Returns the text extracted from the PDF (or the raw file).
2. `resume-worker` calls this endpoint and sends the text to Gemini.

### Option B — Fetch directly from storage
1. Add `storageKey` (or `s3Bucket/s3ObjectKey`) to `AiJobRequestedMessage`.
2. In the worker:
   - If `LOCAL`: read a local file.
   - If `S3`: download the file using the AWS SDK.
3. Extract text from the PDF and build the prompt.

---

## 7) PDF text extraction (suggestion)

Add Apache PDFBox to `resume-worker`:

```xml
<dependency>
  <groupId>org.apache.pdfbox</groupId>
  <artifactId>pdfbox</artifactId>
  <version>3.0.2</version>
</dependency>
```

Basic usage:

```java
try (PDDocument doc = PDDocument.load(inputStream)) {
    PDFTextStripper stripper = new PDFTextStripper();
    String text = stripper.getText(doc);
}
```

---

## 8) Expected format for Gemini

The worker sends a prompt and expects **valid JSON** in response:

```json
{
  "summary": "short summary",
  "strengths": ["strength 1", "strength 2"],
  "improvements": ["improvement 1", "improvement 2"]
}
```

If Gemini returns text outside of JSON, the worker ignores it and falls back.

---

## 9) Final checklist

✅ API Key configured  
✅ Gemini enabled in YAML  
✅ Resume content available to the worker  
✅ End-to-end test with Kafka + MongoDB  

---

## 10) Next improvements (optional)

- Store the **extracted text** in cache for reprocessing.
- Adjust the prompt by segment (e.g., technology, finance, healthcare).
- Add a score evaluation by category (e.g., clarity, impact).
