package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProgressPromptBuilderTest {

    private final AiProgressPromptBuilder builder =
            new AiProgressPromptBuilder(12000, new PromptTemplateLoader());

    @Test
    void buildUsesClasspathTemplateAndInjectsRuntimeValues() {
        AiJobRequestedMessage message = new AiJobRequestedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Language.EN);

        ResumeVersion currentVersion = new ResumeVersion();
        currentVersion.setId(UUID.randomUUID());

        ResumeVersion previousVersion = new ResumeVersion();
        previousVersion.setId(UUID.randomUUID());

        AiFeedbackDocument previousFeedback = new AiFeedbackDocument();
        previousFeedback.setSummary("Needs clearer impact framing.");
        previousFeedback.setStrengths(List.of("Experience: Good backend scope."));
        previousFeedback.setImprovements(List.of("Projects: Explain tradeoffs."));

        String prompt = builder.build(
                message,
                currentVersion,
                previousVersion,
                "Current version text",
                "Previous version text",
                previousFeedback,
                Language.EN);

        assertTrue(prompt.contains("You are a resume reviewer."));
        assertTrue(prompt.contains("Previous feedback delivered to the user"));
        assertTrue(prompt.contains("Current version text"));
        assertTrue(prompt.contains("Previous version text"));
        assertTrue(prompt.contains("Needs clearer impact framing."));
        assertFalse(prompt.contains("{{JOB_ID}}"));
    }
}
