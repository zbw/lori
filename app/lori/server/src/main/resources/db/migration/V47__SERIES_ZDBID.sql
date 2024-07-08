ALTER TABLE item_metadata ADD COLUMN zdb_id_series text default null;
ALTER TABLE item_metadata ADD COLUMN ts_zdb_id_series tsvector GENERATED ALWAYS AS (to_tsvector('english', zdb_id_series)) STORED;
CREATE INDEX ts_zdb_id_series_idx ON item_metadata USING GIN (ts_zdb_id_series);

ALTER TABLE item_metadata RENAME COLUMN zdb_id to zdb_id_journal;
ALTER TABLE item_metadata RENAME COLUMN ts_zdb_id to ts_zdb_id_journal;