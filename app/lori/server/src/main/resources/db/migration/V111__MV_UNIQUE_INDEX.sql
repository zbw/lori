CREATE UNIQUE INDEX mv_metadata_paket_sigel_uidx
    ON mv_metadata_paket_sigel (value);

CREATE UNIQUE INDEX mv_metadata_series_uidx
    ON mv_metadata_is_part_of_series (value);

CREATE UNIQUE INDEX mv_metadata_publication_type_uidx
    ON mv_metadata_publication_type (value);

CREATE UNIQUE INDEX mv_metadata_zdb_ids_uidx
    ON mv_metadata_zdb_ids (value);

CREATE UNIQUE INDEX mv_metadata_licence_url_uidx
    ON mv_metadata_licence_url_filter (value);

CREATE UNIQUE INDEX mv_rights_access_state_uidx
    ON mv_rights_access_state (value);

CREATE UNIQUE INDEX mv_rights_template_name_uidx
    ON mv_rights_template_name (value);

CREATE UNIQUE INDEX mv_rights_licence_contract_uidx
    ON mv_rights_licence_contract (value);

CREATE UNIQUE INDEX mv_rights_zbw_user_agreement_uidx
    ON mv_rights_zbw_user_agreement (value);

CREATE UNIQUE INDEX mv_rights_has_legal_risk_uidx
    ON mv_rights_has_legal_risk (value);
