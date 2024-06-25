DROP INDEX ts_subcom_hdl_idx;
ALTER TABLE item_metadata DROP COLUMN ts_subcom_hdl;
ALTER TABLE item_metadata DROP COLUMN sub_communities_handles;

ALTER TABLE item_metadata ADD COLUMN sub_community_handle text default null;
ALTER TABLE item_metadata ADD COLUMN ts_subcom_hdl tsvector GENERATED ALWAYS AS (to_tsvector('english', sub_community_handle)) STORED;
CREATE INDEX ts_subcom_hdl_idx ON item_metadata USING GIN (ts_subcom_hdl);

ALTER TABLE item_metadata ADD COLUMN sub_community_name text default null;
ALTER TABLE item_metadata ADD COLUMN ts_subcom_name tsvector GENERATED ALWAYS AS (to_tsvector('english', sub_community_name)) STORED;
CREATE INDEX ts_subcom_name_idx ON item_metadata USING GIN (ts_subcom_name);