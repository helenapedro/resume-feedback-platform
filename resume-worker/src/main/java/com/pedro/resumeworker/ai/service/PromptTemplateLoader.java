package com.pedro.resumeworker.ai.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptTemplateLoader {

    public String load(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (var inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt template: " + classpathLocation, ex);
        }
    }
}
