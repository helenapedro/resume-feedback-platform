package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.foundryiq.FoundryIqGroundingProvider;
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
        assertTrue(prompt.contains("Write only in clear English"));
        assertTrue(prompt.contains("Previous feedback delivered to the user"));
        assertTrue(prompt.contains("Current version text"));
        assertTrue(prompt.contains("Previous version text"));
        assertTrue(prompt.contains("Needs clearer impact framing."));
        assertFalse(prompt.contains("{{JOB_ID}}"));
    }

    @Test
    void buildUsesPortugueseTemplateWhenPortugueseIsRequested() {
        AiJobRequestedMessage message = new AiJobRequestedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Language.PT);

        ResumeVersion currentVersion = new ResumeVersion();
        currentVersion.setId(UUID.randomUUID());

        ResumeVersion previousVersion = new ResumeVersion();
        previousVersion.setId(UUID.randomUUID());

        String prompt = builder.build(
                message,
                currentVersion,
                previousVersion,
                "Curriculo atual",
                "Curriculo anterior",
                null,
                Language.PT);

        assertTrue(prompt.contains("revisor especializado em curriculos"));
        assertTrue(prompt.contains("Escreva em portugues"));
        assertTrue(prompt.contains("Feedback anterior: INDISPONIVEL"));
        assertFalse(prompt.contains("Write only in clear English"));
    }

    @Test
    void buildLimitsProgressResumeExcerptsToHalfTheConfiguredResumeLimit() {
        AiProgressPromptBuilder smallBuilder = new AiProgressPromptBuilder(3000, new PromptTemplateLoader());
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

        String prompt = smallBuilder.build(
                message,
                currentVersion,
                previousVersion,
                "current-start-" + "x".repeat(3000) + "-current-end",
                "previous-start-" + "y".repeat(3000) + "-previous-end",
                null,
                Language.EN);

        assertTrue(prompt.contains("per-version progress excerpt limit: 1500"));
        assertTrue(prompt.contains("current-start-"));
        assertTrue(prompt.contains("-current-end"));
        assertTrue(prompt.contains("previous-start-"));
        assertTrue(prompt.contains("-previous-end"));
        assertTrue(prompt.contains("[TRUNCATED MIDDLE: resume excerpt limited for progress analysis]"));
    }

    @Test
    void buildInjectsMicrosoftIqGroundingWhenProviderReturnsContext() {
        AiProgressPromptBuilder groundedBuilder = new AiProgressPromptBuilder(
                12000,
                new PromptTemplateLoader(),
                new TestGroundingProvider("Microsoft IQ / Foundry IQ grounding context: version comparison rubric"));
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

        String prompt = groundedBuilder.build(
                message,
                currentVersion,
                previousVersion,
                "Current version text",
                "Previous version text",
                null,
                Language.EN);

        assertTrue(prompt.contains("Microsoft IQ / Foundry IQ grounding context: version comparison rubric"));
    }

    private record TestGroundingProvider(String context) implements FoundryIqGroundingProvider {
        @Override
        public String feedbackGrounding(Language language, String resumeText) {
            return context;
        }

        @Override
        public String progressGrounding(
                Language language,
                String currentResumeText,
                String previousResumeText,
                AiFeedbackDocument previousFeedback) {
            return context;
        }
    }
}
