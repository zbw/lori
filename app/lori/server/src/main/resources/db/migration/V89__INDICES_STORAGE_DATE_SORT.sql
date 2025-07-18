CREATE INDEX idx_item_metadata_storage_date
    ON item_metadata (storage_date DESC);
CREATE INDEX idx_item_right_id_access_state
    ON item_right (right_id, access_state);
