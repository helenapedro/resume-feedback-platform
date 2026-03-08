package com.pedro.common.ai.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document(collection = "ai_progress")
@TypeAlias("AiProgressDocument")
public class AiProgressDocument {

    @Id
    private String id;

    private UUID jobId;
    private UUID resumeId;
    private UUID resumeVersionId;
    private UUID baselineResumeVersionId;
    private UUID ownerId;
    private Instant createdAt;
    private String model;
    private String promptVersion;
    private String summary;
    private String progressStatus;
    private Integer progressScore;
    private List<String> improvedAreas;
    private List<String> unchangedIssues;
    private List<String> newIssues;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getResumeId() {
        return resumeId;
    }

    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }

    public UUID getResumeVersionId() {
        return resumeVersionId;
    }

    public void setResumeVersionId(UUID resumeVersionId) {
        this.resumeVersionId = resumeVersionId;
    }

    public UUID getBaselineResumeVersionId() {
        return baselineResumeVersionId;
    }

    public void setBaselineResumeVersionId(UUID baselineResumeVersionId) {
        this.baselineResumeVersionId = baselineResumeVersionId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(String progressStatus) {
        this.progressStatus = progressStatus;
    }

    public Integer getProgressScore() {
        return progressScore;
    }

    public void setProgressScore(Integer progressScore) {
        this.progressScore = progressScore;
    }

    public List<String> getImprovedAreas() {
        return improvedAreas;
    }

    public void setImprovedAreas(List<String> improvedAreas) {
        this.improvedAreas = improvedAreas;
    }

    public List<String> getUnchangedIssues() {
        return unchangedIssues;
    }

    public void setUnchangedIssues(List<String> unchangedIssues) {
        this.unchangedIssues = unchangedIssues;
    }

    public List<String> getNewIssues() {
        return newIssues;
    }

    public void setNewIssues(List<String> newIssues) {
        this.newIssues = newIssues;
    }
}
