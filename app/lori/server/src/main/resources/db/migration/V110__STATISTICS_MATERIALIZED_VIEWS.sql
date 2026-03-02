-- ============================================
-- MATERIALIZED VIEW: Metadata Statistics (No Filter)
-- ============================================

CREATE MATERIALIZED VIEW mv_metadata_paket_sigel AS
SELECT
    'paket_sigel' as metric,
    value,
    COUNT(DISTINCT handle) as count
FROM (
         SELECT handle, unnest(paket_sigel) as value
         FROM item_metadata
         WHERE paket_sigel IS NOT NULL
     ) t
GROUP BY value;

CREATE MATERIALIZED VIEW mv_metadata_is_part_of_series AS
SELECT
    'is_part_of_series' as metric,
    unnest(is_part_of_series) as value,
    COUNT(DISTINCT handle) as count
FROM item_metadata
WHERE is_part_of_series IS NOT NULL
GROUP BY unnest(is_part_of_series);


CREATE MATERIALIZED VIEW mv_metadata_publication_type AS
SELECT
    'publication_type' as metric,
    publication_type as value,
    COUNT(DISTINCT handle) as count
FROM item_metadata
WHERE publication_type IS NOT NULL
GROUP BY publication_type;

CREATE MATERIALIZED VIEW mv_metadata_zdb_ids AS
SELECT
    'zdb_ids' as metric,
    unnest(zdb_ids) as value,
    COUNT(DISTINCT handle) as count
FROM item_metadata
WHERE zdb_ids IS NOT NULL
GROUP BY unnest(zdb_ids);

CREATE MATERIALIZED VIEW mv_metadata_licence_url_filter AS
SELECT
    'licence_url_filter' as metric,
    licence_url_filter as value,
    COUNT(DISTINCT handle) as count
FROM item_metadata
WHERE licence_url_filter IS NOT NULL
GROUP BY licence_url_filter;

CREATE MATERIALIZED VIEW mv_rights_access_state AS
SELECT
    'access_state' as metric,
    access_state as value,
    COUNT(DISTINCT handle) as count
FROM item i
         JOIN item_right ir ON i.right_id = ir.right_id
WHERE 'access_state' IS NOT NULL
GROUP BY access_state;

CREATE MATERIALIZED VIEW mv_rights_template_name AS
SELECT
    'template_name' as metric,
    template_name as value,
    COUNT(DISTINCT handle) as count
FROM item i
         JOIN item_right ir ON i.right_id = ir.right_id
WHERE 'template_name' IS NOT NULL
GROUP BY template_name;

CREATE MATERIALIZED VIEW mv_rights_licence_contract AS
SELECT
    'licence_contract' as metric,
    licence_contract as value,
    COUNT(DISTINCT handle) as count
FROM item i
         JOIN item_right ir ON i.right_id = ir.right_id
WHERE licence_contract <> ''
GROUP BY licence_contract;

CREATE MATERIALIZED VIEW mv_rights_zbw_user_agreement AS
SELECT
    'zbw_user_agreement' as metric,
    zbw_user_agreement::text as value,
    COUNT(DISTINCT handle) as count
FROM item i
         JOIN item_right ir ON i.right_id = ir.right_id
WHERE zbw_user_agreement = true
GROUP BY zbw_user_agreement;

CREATE MATERIALIZED VIEW mv_rights_has_legal_risk AS
SELECT
    'has_legal_risk' as metric,
    has_legal_risk::text as value,
    COUNT(DISTINCT handle) as count
FROM item i
         JOIN item_right ir ON i.right_id = ir.right_id
WHERE has_legal_risk = false
GROUP BY has_legal_risk;