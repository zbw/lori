CREATE OR REPLACE FUNCTION custom_lower_array_string(arr text[])
    RETURNS text AS $$
SELECT lower(array_to_string(arr, ' '));  -- Convert array to string and lower case it
$$ LANGUAGE sql IMMUTABLE;

CREATE INDEX doi_lower_idx
    ON item_metadata USING GIN (custom_lower_array_string(doi) gin_trgm_ops);

CREATE INDEX isbn_lower_idx
    ON item_metadata USING GIN (custom_lower_array_string(isbn) gin_trgm_ops);

CREATE INDEX ppn_lower_idx ON item_metadata (LOWER(ppn));
