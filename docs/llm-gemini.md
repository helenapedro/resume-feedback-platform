# Google Gemini (LLM) Integration Guide

Este guia descreve o passo a passo para habilitar o uso do **Google Gemini** no `resume-worker` e para evoluir o pipeline com conteúdo real do currículo.

---

## 1) Pré-requisitos

1. **Conta Google Cloud** com o serviço **Generative Language API** habilitado.
2. **API Key** (não use credenciais de usuário em produção).

> Referência oficial: https://ai.google.dev/

---

## 2) Gerar a API Key

1. Acesse o console da Google Cloud.
2. Vá em **APIs & Services → Credentials**.
3. Clique em **Create Credentials → API Key**.
4. Restrinja a chave (opcional, mas recomendado) para a API **Generative Language**.

---

## 3) Configurar variáveis de ambiente

No ambiente do **resume-worker**:

```bash
export GEMINI_API_KEY="sua_api_key"
```

Em Docker/Kubernetes, adicione a variável no `env` do container ou no Secret Manager.

---

## 4) Habilitar o Gemini no worker

No `resume-worker`, o Gemini já está preparado, mas desabilitado por padrão.

### Em desenvolvimento
Edite `feedback/resume-worker/src/main/resources/application.yml`:

```yaml
app:
  ai-feedback:
    gemini:
      enabled: true
      api-key: ${GEMINI_API_KEY:}
```

### Em produção
O `application-prod.yml` já habilita Gemini se houver `GEMINI_API_KEY`:

```yaml
app:
  ai-feedback:
    gemini:
      enabled: true
      api-key: ${GEMINI_API_KEY:}
```

---

## 5) Ajustar modelo e parâmetros

Os parâmetros são configuráveis via YAML:

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

## 6) Enviar conteúdo real do currículo (passo essencial)

Hoje o worker não recebe o texto do currículo, apenas IDs. Para gerar feedback real:

### Opção A — Buscar conteúdo via API interna
1. Criar um endpoint **interno** no `resume-api` que:
   - Receba `resumeVersionId`
   - Retorne o texto extraído do PDF (ou o arquivo bruto).
2. O `resume-worker` chama esse endpoint e envia o texto para o Gemini.

### Opção B — Buscar direto do storage
1. Adicionar no `AiJobRequestedMessage` o `storageKey` (ou `s3Bucket/s3ObjectKey`).
2. No worker:
   - Se `LOCAL`: ler arquivo local.
   - Se `S3`: baixar arquivo usando SDK AWS.
3. Extrair texto do PDF e montar o prompt.

---

## 7) Extração de texto de PDF (sugestão)

Adicionar o Apache PDFBox ao `resume-worker`:

```xml
<dependency>
  <groupId>org.apache.pdfbox</groupId>
  <artifactId>pdfbox</artifactId>
  <version>3.0.2</version>
</dependency>
```

Uso básico:

```java
try (PDDocument doc = PDDocument.load(inputStream)) {
    PDFTextStripper stripper = new PDFTextStripper();
    String text = stripper.getText(doc);
}
```

---

## 8) Formato esperado pelo Gemini

O worker envia um prompt e espera **JSON válido** de resposta:

```json
{
  "summary": "resumo curto",
  "strengths": ["ponto forte 1", "ponto forte 2"],
  "improvements": ["melhoria 1", "melhoria 2"]
}
```

Se o Gemini retornar texto fora do JSON, o worker ignora e cai no fallback.

---

## 9) Checklist final

✅ API Key configurada  
✅ Gemini habilitado no YAML  
✅ Conteúdo do currículo disponível para o worker  
✅ Teste ponta-a-ponta com Kafka + MongoDB  

---

## 10) Próximos aprimoramentos (opcional)

- Guardar o **texto extraído** em cache para reprocessamentos.
- Ajustar prompt por segmento (ex.: tecnologia, finanças, saúde).
- Adicionar avaliação de score por categoria (ex.: clareza, impacto).
