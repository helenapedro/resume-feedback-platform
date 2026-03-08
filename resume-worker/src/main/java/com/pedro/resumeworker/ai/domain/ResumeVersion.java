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

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "storage_key", length = 1024)
    private String storageKey;

    @Column(name = "s3_bucket", length = 255)
    private String s3Bucket;

    @Column(name = "s3_object_key", length = 1024)
    private String s3ObjectKey;

    @Column(name = "s3_version_id", length = 255)
    private String s3VersionId;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 255)
    private String contentType;
}
