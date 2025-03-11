DROP INDEX zdb_id_series_lower_idx;
DROP INDEX zdb_id_series_idx;
DROP INDEX zdb_id_journal_lower_idx;
DROP INDEX zdb_id_journal_idx;
ALTER TABLE item_metadata
    DROP COLUMN zdb_id_series;
ALTER TABLE item_metadata
    RENAME zdb_id_journal TO zdb_ids;
ALTER TABLE item_metadata
    ALTER COLUMN zdb_ids type text[] using NULLIF(ARRAY [zdb_ids], '{null}');

CREATE OR REPLACE FUNCTION custom_lower_array_string(arr text[])
    RETURNS text AS $$
SELECT lower(array_to_string(arr, ' '));  -- Convert array to string and lower case it
$$ LANGUAGE sql IMMUTABLE;

CREATE INDEX zdb_ids_lower_idx
    ON item_metadata USING GIN (custom_lower_array_string(zdb_ids) gin_trgm_ops);

CREATE INDEX is_part_of_series_lower_idx
    ON item_metadata USING GIN (custom_lower_array_string(is_part_of_series) gin_trgm_ops);

CREATE INDEX licence_url_filter_lower_idx ON item_metadata (LOWER(licence_url_filter));
