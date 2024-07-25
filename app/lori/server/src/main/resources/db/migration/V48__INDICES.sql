CREATE INDEX access_state_idx ON item_right USING btree(access_state);
CREATE INDEX paket_sigel_idx ON item_metadata USING btree(paket_sigel);
CREATE INDEX publication_type_idx ON item_metadata USING btree(publication_type);
CREATE INDEX zdb_id_journal_idx ON item_metadata USING btree(zdb_id_journal);
CREATE INDEX zdb_id_series_idx ON item_metadata USING btree(zdb_id_series);
CREATE INDEX part_of_series_idx ON item_metadata USING btree(is_part_of_series);
CREATE INDEX template_name_idx ON item_right USING btree(template_name);
