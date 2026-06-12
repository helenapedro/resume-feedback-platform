package com.pedro.resumeworker.ai.azureopenai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
class AzureOpenAiSchemaFactory {

    private static final int FEEDBACK_ITEM_COUNT = 3;
    private static final int MIN_PROGRESS_ITEMS = 2;
    private static final int MAX_PROGRESS_ITEMS = 5;
    private static final int MIN_NEW_ISSUES = 0;

    private final ObjectMapper objectMapper;

    AzureOpenAiSchemaFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ObjectNode feedbackSchema() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("summary", stringSchema());
        properties.set("strengths", stringArraySchema(FEEDBACK_ITEM_COUNT, FEEDBACK_ITEM_COUNT));
        properties.set("improvements", stringArraySchema(FEEDBACK_ITEM_COUNT, FEEDBACK_ITEM_COUNT));

        return objectSchema(
                properties,
                "summary",
                "strengths",
                "improvements");
    }

    ObjectNode progressSchema() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("summary", stringSchema());
        properties.set("progressStatus", stringSchema());
        properties.set("progressScore", integerSchema());
        properties.set("improvedAreas", stringArraySchema(MIN_PROGRESS_ITEMS, MAX_PROGRESS_ITEMS));
        properties.set("unchangedIssues", stringArraySchema(MIN_PROGRESS_ITEMS, MAX_PROGRESS_ITEMS));
        properties.set("newIssues", stringArraySchema(MIN_NEW_ISSUES, MAX_PROGRESS_ITEMS));

        return objectSchema(
                properties,
                "summary",
                "progressStatus",
                "progressScore",
                "improvedAreas",
                "unchangedIssues",
                "newIssues");
    }

    private ObjectNode objectSchema(ObjectNode properties, String... requiredFields) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("additionalProperties", objectMapper.getNodeFactory().booleanNode(false));
        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        for (String field : requiredFields) {
            required.add(field);
        }
        schema.set("required", required);
        return schema;
    }

    private ObjectNode stringSchema() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "string");
        return node;
    }

    private ObjectNode integerSchema() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "integer");
        return node;
    }

    private ObjectNode stringArraySchema(int minItems, int maxItems) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "array");
        node.put("minItems", minItems);
        node.put("maxItems", maxItems);
        node.set("items", stringSchema());
        return node;
    }
}
