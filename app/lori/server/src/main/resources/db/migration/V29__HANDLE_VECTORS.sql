ALTER TABLE item_metadata ADD COLUMN ts_col_hdl tsvector GENERATED ALWAYS AS (to_tsvector('english', collection_handle)) STORED;
CREATE INDEX ts_col_hdl_idx ON item_metadata USING GIN (ts_col_hdl);

ALTER TABLE item_metadata ADD COLUMN ts_com_hdl tsvector GENERATED ALWAYS AS (to_tsvector('english', community_handle)) STORED;
CREATE INDEX ts_com_hdl_idx ON item_metadata USING GIN (ts_com_hdl);

ALTER TABLE item_metadata ADD COLUMN ts_subcom_hdl tsvector GENERATED ALWAYS AS (array_to_tsvector(sub_communities_handles)) STORED;
CREATE INDEX ts_subcom_hdl_idx ON item_metadata USING GIN (ts_subcom_hdl);