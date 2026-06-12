package com.pedro.resumeworker.ai.foundryiq;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
class LocalFoundryIqKnowledgeSource {

    private static final String RESOURCE_PATH = "foundry-iq/resume-review-knowledge.md";

    List<FoundryIqKnowledgeSource> retrieve() {
        try {
            String content = new ClassPathResource(RESOURCE_PATH)
                    .getContentAsString(StandardCharsets.UTF_8)
                    .trim();
            return List.of(new FoundryIqKnowledgeSource(
                    "Resume Review Knowledge Base",
                    content,
                    "classpath:" + RESOURCE_PATH));
        } catch (IOException ex) {
            return List.of();
        }
    }
}
