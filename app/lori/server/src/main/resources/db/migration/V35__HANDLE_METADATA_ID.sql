ALTER TABLE item_metadata ADD COLUMN ts_metadata_id tsvector GENERATED ALWAYS AS (to_tsvector('english', metadata_id)) STORED;
CREATE INDEX ts_metadata_id_idx ON item_metadata USING GIN (ts_metadata_id);
