-- ============================================
-- ITEM TABLE INDICES
-- ============================================

-- Critical for joining item -> item_metadata
CREATE INDEX IF NOT EXISTS idx_item_handle
    ON item(handle);

-- Critical for joining item -> item_right
CREATE INDEX IF NOT EXISTS idx_item_right_id
    ON item(right_id);

-- Composite index for faster joins (optional but recommended)
CREATE INDEX IF NOT EXISTS idx_item_handle_right_id
    ON item(handle, right_id);


-- ============================================
-- ITEM_METADATA TABLE INDICES
-- ============================================

-- Critical for joins from item -> item_metadata
CREATE INDEX IF NOT EXISTS idx_item_metadata_handle
    ON item_metadata(handle);

-- Critical for CASE 2: Metadata filtering
-- Use functional index for case-insensitive search
CREATE INDEX IF NOT EXISTS idx_item_metadata_publication_type_lower
    ON item_metadata(lower(publication_type));

-- If you filter by other metadata columns, add them too:
CREATE INDEX IF NOT EXISTS idx_item_metadata_paket_sigel
    ON item_metadata(paket_sigel)
    WHERE paket_sigel IS NOT NULL;

-- For array columns with frequent filtering (if needed)
CREATE INDEX IF NOT EXISTS idx_item_metadata_is_part_of_series_gin
    ON item_metadata USING GIN(is_part_of_series);

CREATE INDEX IF NOT EXISTS idx_item_metadata_zdb_ids_gin
    ON item_metadata USING GIN(zdb_ids);


-- ============================================
-- ITEM_RIGHT TABLE INDICES
-- ============================================

-- Critical for joining item -> item_right (if not already primary key)
CREATE INDEX IF NOT EXISTS idx_item_right_right_id
    ON item_right(right_id);

-- Critical for CASE 3: Rights filtering
-- Partial index for non-empty licence_contract
CREATE INDEX IF NOT EXISTS idx_item_right_licence_contract
    ON item_right(licence_contract)
    WHERE licence_contract <> '';

-- Indices for item_right columns used in statistics
CREATE INDEX IF NOT EXISTS idx_item_right_access_state
    ON item_right(access_state)
    WHERE access_state IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_item_right_template_name
    ON item_right(template_name)
    WHERE template_name IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_item_right_zbw_user_agreement
    ON item_right(zbw_user_agreement)
    WHERE zbw_user_agreement IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_item_right_has_legal_risk
    ON item_right(has_legal_risk)
    WHERE has_legal_risk IS NOT NULL;

-- For the complex restricted_open_content_licence query
CREATE INDEX IF NOT EXISTS idx_item_right_restricted_open
    ON item_right(restricted_open_content_licence)
    WHERE restricted_open_content_licence IS NOT NULL;