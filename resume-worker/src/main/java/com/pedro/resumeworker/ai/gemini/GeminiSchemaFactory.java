package com.pedro.resumeworker.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
class GeminiSchemaFactory {

    private static final int FEEDBACK_ITEM_COUNT = 3;
    private static final int PROGRESS_SCORE_MIN = 0;
    private static final int PROGRESS_SCORE_MAX = 100;

    private final ObjectMapper objectMapper;

    GeminiSchemaFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ObjectNode feedbackSchema() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("summary", typedSchema("STRING"));
        properties.set("strengths", stringArraySchema(FEEDBACK_ITEM_COUNT, FEEDBACK_ITEM_COUNT));
        properties.set("improvements", stringArraySchema(FEEDBACK_ITEM_COUNT, FEEDBACK_ITEM_COUNT));

        return objectSchema(properties, "summary", "strengths", "improvements");
    }

    ObjectNode progressSchema() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("summary", typedSchema("STRING"));
        properties.set("progressStatus", typedSchema("STRING"));
        properties.set("progressScore", progressScoreSchema());
        properties.set("improvedAreas", stringArraySchema());
        properties.set("unchangedIssues", stringArraySchema());
        properties.set("newIssues", stringArraySchema());

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
        schema.put("type", "OBJECT");
        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        for (String field : requiredFields) {
            required.add(field);
        }
        schema.set("required", required);
        return schema;
    }

    private ObjectNode progressScoreSchema() {
        ObjectNode node = typedSchema("INTEGER");
        node.put("minimum", PROGRESS_SCORE_MIN);
        node.put("maximum", PROGRESS_SCORE_MAX);
        return node;
    }

    private ObjectNode typedSchema(String type) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type);
        return node;
    }

    private ObjectNode stringArraySchema() {
        ObjectNode array = typedSchema("ARRAY");
        array.set("items", typedSchema("STRING"));
        return array;
    }

    private ObjectNode stringArraySchema(int minItems, int maxItems) {
        ObjectNode array = stringArraySchema();
        array.put("minItems", minItems);
        array.put("maxItems", maxItems);
        return array;
    }
}
