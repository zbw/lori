create table sessions(
    session_id text,
    authenticated bool not null,
    first_name text,
    last_name text,
    role role_enum not null,
    valid_until timestamptz not null,
    primary key (session_id)
);