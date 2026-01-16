ALTER TABLE resumes
    ADD CONSTRAINT fk_resumes_current_version
    FOREIGN KEY (current_version_id) REFERENCES resume_versions(id);
