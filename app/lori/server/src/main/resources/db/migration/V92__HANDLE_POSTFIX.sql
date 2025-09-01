-- 1. Add a generated column for the postfix number
ALTER TABLE item_metadata
    ADD COLUMN handle_postfix INT
        GENERATED ALWAYS AS ((split_part(handle, '/', 2))::INT) STORED;

-- 2. Add an index on the generated column for descending sort performance
CREATE INDEX idx_item_metadata_handle_postfix_desc
    ON item_metadata (handle_postfix DESC);
