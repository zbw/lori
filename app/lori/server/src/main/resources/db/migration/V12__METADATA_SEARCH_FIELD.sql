ALTER TABLE item_metadata ADD COLUMN ts_collection tsvector GENERATED ALWAYS AS (to_tsvector('english', collection_name)) STORED;
CREATE INDEX ts_collection_idx ON item_metadata USING GIN (ts_collection);

ALTER TABLE item_metadata ADD COLUMN ts_community tsvector GENERATED ALWAYS AS (to_tsvector('english', community_name)) STORED;
CREATE INDEX ts_community_idx ON item_metadata USING GIN (ts_community);

ALTER TABLE item_metadata ADD COLUMN ts_sigel tsvector GENERATED ALWAYS AS (to_tsvector('english', paket_sigel)) STORED;
CREATE INDEX ts_sigel_idx ON item_metadata USING GIN (ts_sigel);

ALTER TABLE item_metadata ADD COLUMN ts_zbd_id tsvector GENERATED ALWAYS AS (to_tsvector('english', zbd_id)) STORED;
CREATE INDEX ts_zbd_id_idx ON item_metadata USING GIN (ts_zbd_id);