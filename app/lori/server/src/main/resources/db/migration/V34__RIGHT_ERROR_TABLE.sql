create table right_error(
    error_id                serial primary key,
    created_on              timestamptz,
    conflict_type           text,
    conflicting_right_id    text,
    handle_id               text,
    message                 text,
    metadata_id             text,
    right_id_source         text,
    template_id_source      integer
);
