ALTER TABLE item_metadata RENAME COLUMN ts_zbd_id TO ts_zdb_id;
ALTER INDEX ts_zbd_id_idx RENAME TO ts_zdb_id_idx;
ALTER TABLE item_metadata ADD COLUMN ts_title tsvector GENERATED ALWAYS AS (to_tsvector('english', title)) STORED;
CREATE INDEX ts_title_idx ON item_metadata USING GIN (ts_title);
