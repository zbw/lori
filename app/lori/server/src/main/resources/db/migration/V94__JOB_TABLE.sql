CREATE TABLE export_jobs (
    id TEXT PRIMARY KEY,
    status TEXT NOT NULL,
    created_on TIMESTAMP NOT NULL,
    created_by TEXT NOT NULL,
    last_updated_on TEXT NOT NULL,
    error_message TEXT,
    file_path TEXT,
    search_term TEXT NOT NULL,
    export_format TEXT NOT NULL
);