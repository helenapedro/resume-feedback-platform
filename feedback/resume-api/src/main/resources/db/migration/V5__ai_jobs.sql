CREATE TABLE ai_jobs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    resume_version_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(80) NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,

    CONSTRAINT fk_ai_jobs_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions(id),
    CONSTRAINT uk_ai_jobs_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_ai_jobs_version ON ai_jobs(resume_version_id);
CREATE INDEX idx_ai_jobs_status ON ai_jobs(status);
