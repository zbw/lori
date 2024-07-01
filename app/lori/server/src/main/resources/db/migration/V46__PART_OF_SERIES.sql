ALTER TABLE item_metadata ADD COLUMN is_part_of_series text default null;
ALTER TABLE item_metadata ADD COLUMN ts_is_part_of_series tsvector GENERATED ALWAYS AS (to_tsvector('english', is_part_of_series)) STORED;
CREATE INDEX ts_is_part_of_series_idx ON item_metadata USING GIN (ts_is_part_of_series);
