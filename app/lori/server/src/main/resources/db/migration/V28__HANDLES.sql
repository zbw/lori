ALTER TABLE item_metadata ADD COLUMN sub_communities_handles text[] default null;
ALTER TABLE item_metadata ADD COLUMN community_handle text default null;
ALTER TABLE item_metadata ADD COLUMN collection_handle text default null;
ALTER TABLE item_metadata DROP COLUMN sub_communities;
