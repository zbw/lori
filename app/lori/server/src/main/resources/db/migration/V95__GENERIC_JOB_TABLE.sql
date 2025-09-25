alter table export_jobs alter column last_updated_on type TIMESTAMP USING last_updated_on::TIMESTAMP;

CREATE TABLE generic_jobs (
    id TEXT PRIMARY KEY,
    status TEXT NOT NULL,
    kind TEXT NOT NULL,
    created_on TIMESTAMP NOT NULL,
    created_by TEXT NOT NULL,
    last_updated_on TIMESTAMP NOT NULL,
    error_message TEXT,
    summary TEXT NOT NULL
);
