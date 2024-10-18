-- 1. DROP FK constraint
ALTER TABLE item DROP CONSTRAINT metadata_id_fkey;
-- 2. Replace data of metadata_id column with those from handle
UPDATE item T SET metadata_id = (SELECT handle FROM item_metadata WHERE metadata_id = T.metadata_id);
-- 3. Rename item column
ALTER TABLE item RENAME metadata_id TO handle;
-- 4. Drop original primary key
ALTER TABLE item_metadata DROP CONSTRAINT access_right_header_pkey;
-- 5. Create unique index on handle
CREATE UNIQUE INDEX handle_uniq_idx ON item_metadata (handle);
-- 6. Create new primary key using existing index for handle
ALTER TABLE item_metadata ADD PRIMARY KEY USING INDEX handle_uniq_idx;
-- 7. Drop depending columns and indices
DROP INDEX ts_metadata_id_idx;
ALTER TABLE item_metadata DROP COLUMN ts_metadata_id;
-- 7. Drop metadata_id column
ALTER TABLE item_metadata DROP COLUMN metadata_id;
-- 8. Add FK constraint to item
ALTER TABLE item ADD CONSTRAINT handle_fkey FOREIGN KEY (handle) REFERENCES item_metadata (handle) ON DELETE CASCADE;
