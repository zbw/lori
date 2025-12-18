CREATE INDEX item_metadata_zdb_ids_gin_idx ON item_metadata USING gin(zdb_ids);
CREATE INDEX item_metadata_sigel_gin_idx ON item_metadata USING gin(paket_sigel);
