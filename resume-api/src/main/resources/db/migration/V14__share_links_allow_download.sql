ALTER TABLE share_links
    ADD COLUMN allow_download BOOLEAN NOT NULL DEFAULT TRUE AFTER permission;
