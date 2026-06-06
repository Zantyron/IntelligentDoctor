USE doctor;

ALTER TABLE import_job
    ADD COLUMN storage_path VARCHAR(500) NULL AFTER status,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER storage_path;
