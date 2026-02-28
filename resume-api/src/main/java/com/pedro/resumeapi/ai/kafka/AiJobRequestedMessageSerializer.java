package com.pedro.resumeapi.ai.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedro.common.ai.AiJobRequestedMessage;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class AiJobRequestedMessageSerializer implements Serializer<AiJobRequestedMessage> {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public byte[] serialize(String topic, AiJobRequestedMessage data) {
        if (data == null) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException ex) {
            throw new SerializationException("Failed to serialize AiJobRequestedMessage.", ex);
        }
    }
}
