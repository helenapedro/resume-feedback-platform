package com.pedro.resumeworker.ai.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document(collection = "ai_feedback")
@Getter
@Setter
public class AiFeedbackDocument {

    @Id
    private String id;

    private UUID jobId;
    private UUID resumeId;
    private UUID resumeVersionId;
    private UUID ownerId;
    private Instant createdAt;
    private String model;
    private String promptVersion;
    private String summary;
    private List<String> strengths;
    private List<String> improvements;
}
