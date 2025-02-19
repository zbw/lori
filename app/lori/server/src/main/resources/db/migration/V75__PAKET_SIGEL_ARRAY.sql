DROP INDEX paket_sigel_lower_idx;
DROP INDEX paket_sigel_idx;
ALTER TABLE item_metadata
    ALTER COLUMN paket_sigel type text[] using NULLIF(ARRAY[paket_sigel], '{null}');
CREATE INDEX paket_sigel_idx on item_metadata USING GIN (paket_sigel);