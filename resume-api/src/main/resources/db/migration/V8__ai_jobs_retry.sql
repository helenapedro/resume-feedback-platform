ALTER TABLE ai_jobs
    ADD COLUMN error_code VARCHAR(80) NULL,
    ADD COLUMN error_detail VARCHAR(1024) NULL,
    ADD COLUMN next_retry_at TIMESTAMP NULL;

CREATE INDEX idx_ai_jobs_next_retry ON ai_jobs(next_retry_at);
