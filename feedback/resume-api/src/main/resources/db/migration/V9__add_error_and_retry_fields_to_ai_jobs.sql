ALTER TABLE ai_jobs
    ADD COLUMN error_code VARCHAR(80) NULL AFTER finished_at,
    ADD COLUMN error_detail VARCHAR(1024) NULL AFTER error_code,
    ADD COLUMN next_retry_at TIMESTAMP NULL AFTER error_detail;

CREATE INDEX idx_ai_jobs_status_retry
    ON ai_jobs(status, next_retry_at);
