CREATE TABLE ai_progress_refs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    resume_version_id BINARY(16) NOT NULL,
    baseline_resume_version_id BINARY(16) NOT NULL,
    progress_version INT NOT NULL DEFAULT 1,
    mongo_doc_id VARCHAR(64) NOT NULL,
    model VARCHAR(80) NULL,
    prompt_version VARCHAR(40) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_progress_refs_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions(id),
    CONSTRAINT fk_progress_refs_baseline_version FOREIGN KEY (baseline_resume_version_id) REFERENCES resume_versions(id),
    CONSTRAINT uk_progress_ref_unique UNIQUE (resume_version_id, progress_version)
);

CREATE INDEX idx_progress_refs_version ON ai_progress_refs(resume_version_id);
