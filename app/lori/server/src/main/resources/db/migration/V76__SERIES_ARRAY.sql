DROP INDEX is_part_of_series_lower_idx;
DROP INDEX part_of_series_idx;
ALTER TABLE item_metadata
    ALTER COLUMN is_part_of_series type text[] using NULLIF(ARRAY[is_part_of_series], '{null}');
CREATE INDEX is_part_of_series_idx on item_metadata USING GIN (is_part_of_series);