CREATE INDEX idx_ai_jobs_status_retry
    ON ai_jobs(status, next_retry_at);
