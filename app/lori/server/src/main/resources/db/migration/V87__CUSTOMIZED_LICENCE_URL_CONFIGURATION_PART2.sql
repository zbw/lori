DROP INDEX ts_licence_url_idx;
ALTER TABLE item_metadata DROP COLUMN ts_licence_url;
ALTER TABLE item_metadata
    ADD COLUMN ts_licence_url tsvector
        GENERATED ALWAYS AS (
            to_tsvector(
                    'simple',
                    regexp_replace(
                            regexp_replace(
                                    lower(licence_url),
                                    '[-_]', '', 'g' -- remove hyphens and underscores
                            ),
                            'https?://(www\.)?|[/.]+', ' ', 'g'
                    )
            )
            ) STORED;

CREATE INDEX ts_licence_url_idx
    ON item_metadata
        USING GIN (ts_licence_url);