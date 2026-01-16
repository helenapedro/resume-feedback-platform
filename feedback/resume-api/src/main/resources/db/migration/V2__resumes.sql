CREATE TABLE resumes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    current_version_id BINARY(16) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_resumes_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE INDEX idx_resumes_owner_created ON resumes(owner_id, created_at);
CREATE INDEX idx_resumes_current_version ON resumes(current_version_id);
