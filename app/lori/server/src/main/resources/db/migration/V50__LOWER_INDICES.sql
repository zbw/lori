CREATE INDEX paket_sigel_lower_idx ON item_metadata USING btree(lower(paket_sigel));
CREATE INDEX zdb_id_journal_lower_idx ON item_metadata USING btree(lower(zdb_id_journal));
CREATE INDEX zdb_id_series_lower_idx ON item_metadata USING btree(lower(zdb_id_series));
CREATE INDEX is_part_of_series_lower_idx ON item_metadata USING btree(lower(is_part_of_series));
