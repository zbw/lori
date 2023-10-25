ALTER TABLE item_metadata ADD COLUMN ts_hdl tsvector GENERATED ALWAYS AS (to_tsvector('english', handle)) STORED;
CREATE INDEX ts_hdl_idx ON item_metadata USING GIN (ts_hdl);
