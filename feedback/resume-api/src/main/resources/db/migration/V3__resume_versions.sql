CREATE TABLE resume_versions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    resume_id BINARY(16) NOT NULL,
    version_number INT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size_bytes BIGINT NULL,

    -- storage (local now, S3 later)
    storage_key VARCHAR(1024) NOT NULL,

    -- S3 metadata (for later)
    s3_bucket VARCHAR(255) NULL,
    s3_object_key VARCHAR(1024) NULL,
    s3_version_id VARCHAR(255) NULL,

    checksum_sha256 CHAR(64) NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BINARY(16) NULL,

    CONSTRAINT fk_versions_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    CONSTRAINT uk_resume_version UNIQUE (resume_id, version_number)
);
CREATE INDEX idx_versions_resume_ver ON resume_versions(resume_id, version_number);
CREATE INDEX idx_versions_created_by ON resume_versions(created_by);
