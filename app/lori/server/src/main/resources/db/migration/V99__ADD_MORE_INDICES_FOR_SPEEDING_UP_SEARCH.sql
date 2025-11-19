CREATE INDEX idx_item_metadata_publication_type_handle
    ON item_metadata (publication_type, handle);

CREATE INDEX idx_item_metadata_paket_sigel_handle
    ON item_metadata (paket_sigel, handle);

CREATE INDEX idx_item_metadata_zdb_ids_handle
    ON item_metadata (zdb_ids, handle);

CREATE INDEX idx_item_metadata_part_of_series_handle
    ON item_metadata (is_part_of_series, handle);

CREATE INDEX idx_item_licence_url_handle
    ON item_metadata (licence_url_filter, handle);
