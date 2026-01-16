CREATE TABLE share_links (
    id BINARY(16) NOT NULL PRIMARY KEY,
    resume_id BINARY(16) NOT NULL,
    token_hash CHAR(64) NOT NULL, -- store SHA-256 of token
    permission VARCHAR(20) NOT NULL, -- VIEW or COMMENT
    expires_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    max_uses INT NULL,
    use_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BINARY(16) NULL,

    CONSTRAINT fk_share_links_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    CONSTRAINT uk_share_links_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_share_links_resume ON share_links(resume_id);
CREATE INDEX idx_share_links_expires ON share_links(expires_at);

CREATE TABLE access_audit (
    id BINARY(16) NOT NULL PRIMARY KEY,
    share_link_id BINARY(16) NOT NULL,
    resume_id BINARY(16) NOT NULL,
    resume_version_id BINARY(16) NULL,
    event_type VARCHAR(40) NOT NULL, -- OPEN_LINK, COMMENT_ATTEMPT, COMMENT_CREATED...
    ip_address VARCHAR(60) NULL,
    user_agent VARCHAR(512) NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    failure_reason VARCHAR(255) NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_share_link FOREIGN KEY (share_link_id) REFERENCES share_links(id),
    CONSTRAINT fk_audit_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    CONSTRAINT fk_audit_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions(id)
);

CREATE INDEX idx_audit_share_link ON access_audit(share_link_id, occurred_at);
CREATE INDEX idx_audit_resume ON access_audit(resume_id, occurred_at);
