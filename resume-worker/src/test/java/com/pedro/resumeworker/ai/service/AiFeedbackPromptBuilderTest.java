package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiFeedbackPromptBuilderTest {

    private final AiFeedbackPromptBuilder builder = new AiFeedbackPromptBuilder(12000);

    @Test
    void buildCreatesRecruiterFocusedPromptWithSharperOutputContract() {
        AiJobRequestedMessage message = new AiJobRequestedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Language.EN);

        String prompt = builder.build(message, "Led platform redesign and improved latency by 40 percent.", Language.EN);

        assertTrue(prompt.contains("senior technical recruiter"));
        assertTrue(prompt.contains("what tier the resume reads at today"));
        assertTrue(prompt.contains("3 highest-leverage fixes"));
        assertTrue(prompt.contains("Each item must state the problem, what to change in the resume, and why"));
        assertTrue(prompt.contains("Start every item with a target area"));
        assertTrue(prompt.contains("Resume extracted (raw text):"));
    }
}
