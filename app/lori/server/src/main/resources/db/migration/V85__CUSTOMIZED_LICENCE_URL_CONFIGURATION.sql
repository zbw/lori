ALTER TABLE item_metadata DROP COLUMN ts_licence_url;
ALTER TABLE item_metadata
    ADD COLUMN ts_licence_url tsvector
        GENERATED ALWAYS AS (
            to_tsvector(
                    'simple',
                    regexp_replace(licence_url, 'https?://(www\.)?|[/.]+', ' ', 'g')
            )
            ) STORED;

CREATE INDEX ts_licence_url_idx
    ON item_metadata
        USING GIN (ts_licence_url);