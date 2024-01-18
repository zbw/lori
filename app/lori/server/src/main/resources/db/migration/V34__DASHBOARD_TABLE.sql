create table right_error(
    error_id                serial primary key,
    metadata_id             text,
    handle_id               text,
    right_id                text,
    conflicting_right_id    text,
    description             text,
    created_on              timestamptz
);