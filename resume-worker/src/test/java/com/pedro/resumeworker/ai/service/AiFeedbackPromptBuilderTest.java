package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiFeedbackPromptBuilderTest {

    private final AiFeedbackPromptBuilder builder =
            new AiFeedbackPromptBuilder(12000, new PromptTemplateLoader());

    @Test
    void buildUsesClasspathTemplateAndInjectsFeedbackPromptValues() {
        AiJobRequestedMessage message = new AiJobRequestedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Language.EN);

        String prompt = builder.build(message, "Led platform redesign and improved latency by 40 percent.", Language.EN);

        assertTrue(prompt.contains("senior technical recruiter"));
        assertTrue(prompt.contains("Write only in English"));
        assertTrue(prompt.contains("what tier the resume reads at today"));
        assertTrue(prompt.contains("4 or 5 strengths"));
        assertTrue(prompt.contains("Each item must state the problem, what to change in the resume, and why"));
        assertTrue(prompt.contains("Start every item with a target area"));
        assertTrue(prompt.contains("Resume extracted (raw text):"));
        assertFalse(prompt.contains("{{JOB_ID}}"));
    }

    @Test
    void buildAlwaysUsesEnglishTemplateEvenWhenPortugueseIsRequested() {
        AiJobRequestedMessage message = new AiJobRequestedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Language.PT);

        String prompt = builder.build(message, "Experiencia em analytics.", Language.PT);

        assertTrue(prompt.contains("Write only in English"));
        assertTrue(prompt.contains("Resume extracted (raw text):"));
        assertFalse(prompt.contains("Curriculo extraido"));
    }

    @Test
    void buildTruncatesResumeTextAtConfiguredLimit() {
        AiFeedbackPromptBuilder smallBuilder = new AiFeedbackPromptBuilder(12, new PromptTemplateLoader());
        AiJobRequestedMessage message = new AiJobRequestedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Language.EN);

        String prompt = smallBuilder.build(message, "abcdefghijklXYZTAIL", Language.EN);

        assertTrue(prompt.contains("abcdefghijkl"));
        assertTrue(prompt.contains("[TRUNCATED: resume text exceeded configured analysis limit]"));
        assertFalse(prompt.contains("XYZTAIL"));
    }
}
