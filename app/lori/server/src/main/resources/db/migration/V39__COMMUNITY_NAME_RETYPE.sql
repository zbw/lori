drop index ts_community_idx;
alter table item_metadata drop column ts_community;
alter table item_metadata alter column community_name type text;
ALTER TABLE item_metadata ADD COLUMN ts_community tsvector GENERATED ALWAYS AS (to_tsvector('english', community_name)) STORED;
CREATE INDEX ts_community_idx ON item_metadata USING GIN (ts_community);
