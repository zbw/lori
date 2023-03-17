create table bookmark(
    bookmark_id serial primary key,
    bookmark_name text not null,
    search_term text,
    filter_publication_date text,
    filter_access_state text,
    filter_temporal_validity text,
    filter_start_date text,
    filter_end_date text,
    filter_formal_rule text,
    filter_valid_on text,
    filter_paket_sigel text,
    filter_zdb_id text,
    filter_no_right_information bool
)