CREATE INDEX trgm_collection_idx ON item_metadata USING GIST (collection_name gist_trgm_ops);
CREATE INDEX trgm_community_idx ON item_metadata USING GIST (community_name gist_trgm_ops);
CREATE INDEX trgm_sigel_idx ON item_metadata USING GIST (paket_sigel gist_trgm_ops);
CREATE INDEX trgm_zdb_idx ON item_metadata USING GIST (zdb_id gist_trgm_ops);