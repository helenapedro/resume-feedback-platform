package com.pedro.resumeworker.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "resume_versions")
@Getter
@Setter
public class ResumeVersion {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "resume_id", columnDefinition = "BINARY(16)")
    private UUID resumeId;

    @Column(name = "created_by", columnDefinition = "BINARY(16)")
    private UUID createdBy;
}
