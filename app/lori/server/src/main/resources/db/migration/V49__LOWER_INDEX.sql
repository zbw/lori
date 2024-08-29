CREATE INDEX publication_type_lower_idx ON item_metadata USING btree(lower(publication_type));
DROP INDEX ts_zdb_id_idx;
DROP INDEX ts_zdb_id_series_idx;
DROP INDEX ts_sigel_idx;
DROP INDEX ts_is_part_of_series_idx;
ALTER TABLE item_metadata DROP COLUMN ts_zdb_id_series;
ALTER TABLE item_metadata DROP COLUMN ts_zdb_id_journal;
ALTER TABLE item_metadata DROP COLUMN ts_sigel;
ALTER TABLE item_metadata DROP COLUMN ts_is_part_of_series;
