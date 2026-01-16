CREATE TABLE comments (
                          id BINARY(16) NOT NULL PRIMARY KEY,
                          resume_version_id BINARY(16) NOT NULL,

                          author_user_id BINARY(16) NULL,       -- nullable for guest
                          author_label VARCHAR(120) NOT NULL,   -- e.g., "Guest" or user display
                          body TEXT NOT NULL,

                          anchor_ref VARCHAR(255) NULL,         -- page/section/offset
                          parent_comment_id BINARY(16) NULL,    -- thread / reply

                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NULL,

                          CONSTRAINT fk_comments_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions(id),
                          CONSTRAINT fk_comments_author FOREIGN KEY (author_user_id) REFERENCES users(id),
                          CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id)
);

CREATE INDEX idx_comments_version_created ON comments(resume_version_id, created_at);
CREATE INDEX idx_comments_parent ON comments(parent_comment_id);

CREATE TABLE ai_feedback_refs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    resume_version_id BINARY(16) NOT NULL,
    feedback_version INT NOT NULL DEFAULT 1,
    mongo_doc_id VARCHAR(64) NOT NULL,
    model VARCHAR(80) NULL,
    prompt_version VARCHAR(40) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_feedback_refs_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions(id),
    CONSTRAINT uk_feedback_ref_unique UNIQUE (resume_version_id, feedback_version)
);

CREATE INDEX idx_feedback_refs_version ON ai_feedback_refs(resume_version_id);
