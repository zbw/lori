create table access_right_header
(
    header_id      varchar(255) not null,
    tenant         varchar(255),
    usage_guide    varchar(255),
    template       varchar(255),
    mention        bool,
    sharealike     bool,
    commercial_use bool,
    copyright      bool,
    primary key (header_id)
);

create table access_right_action
(
    action_id  serial primary key,
    type       varchar(32) not null,
    permission bool        not null,
    header_id  varchar(255),
    constraint access_right_header_header_id_fkey foreign key (header_id) REFERENCES access_right_header (header_id)
);

create table access_right_restriction
(
    restriction_id   serial primary key,
    type             varchar(32)   not null,
    attribute_type   varchar(32)   not null,
    attribute_values varchar(1024) not null,
    action_id        serial,
    constraint access_right_restriction_action_id_fkey foreign key (action_id) REFERENCES access_right_action (action_id)
);

grant select on access_right_header,access_right_action,access_right_restriction to public;
create index access_right_action_access_right_id_idx on access_right_action (header_id);
create index access_right_restriction_action_id_idx on access_right_restriction (action_id);
