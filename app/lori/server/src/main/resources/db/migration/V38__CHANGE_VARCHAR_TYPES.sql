alter table item_metadata alter column author type text;

drop index ts_collection_idx;
alter table item_metadata drop column ts_collection;
alter table item_metadata alter column collection_name type text;
ALTER TABLE item_metadata ADD COLUMN ts_collection tsvector GENERATED ALWAYS AS (to_tsvector('english', collection_name)) STORED;
CREATE INDEX ts_collection_idx ON item_metadata USING GIN (ts_collection);

drop index ts_com_hdl_idx;
alter table item_metadata drop column ts_com_hdl;
alter table item_metadata alter column community_handle type text;
ALTER TABLE item_metadata ADD COLUMN ts_com_hdl tsvector GENERATED ALWAYS AS (to_tsvector('english', community_handle)) STORED;
CREATE INDEX ts_com_hdl_idx ON item_metadata USING GIN (ts_com_hdl);

drop index ts_hdl_idx;
alter table item_metadata drop column ts_hdl;
alter table item_metadata alter column handle type text;
ALTER TABLE item_metadata ADD COLUMN ts_hdl tsvector GENERATED ALWAYS AS (to_tsvector('english', handle)) STORED;
CREATE INDEX ts_hdl_idx ON item_metadata USING GIN (ts_hdl);

drop index ts_zdb_id_idx;
ALTER TABLE item_metadata drop COLUMN ts_zdb_id;
alter table item_metadata alter column zdb_id type text;
ALTER TABLE item_metadata ADD COLUMN ts_zdb_id tsvector GENERATED ALWAYS AS (to_tsvector('english', zdb_id)) STORED;
CREATE INDEX ts_zdb_id_idx ON item_metadata USING GIN (ts_zdb_id);

alter table item_metadata alter column ppn type text;

drop index ts_title_idx;
alter table item_metadata drop column ts_title;
alter table item_metadata alter column title type text;
ALTER TABLE item_metadata ADD COLUMN ts_title tsvector GENERATED ALWAYS AS (to_tsvector('english', title)) STORED;
CREATE INDEX ts_title_idx ON item_metadata USING GIN (ts_title);

alter table item_metadata alter column title_journal type text;
alter table item_metadata alter column title_series type text;
alter table item_metadata alter column rights_k10plus type text;

drop index ts_sigel_idx;
alter table item_metadata drop column ts_sigel;
alter table item_metadata alter column paket_sigel type text;
ALTER TABLE item_metadata ADD COLUMN ts_sigel tsvector GENERATED ALWAYS AS (to_tsvector('english', paket_sigel)) STORED;
CREATE INDEX ts_sigel_idx ON item_metadata USING GIN (ts_sigel);
