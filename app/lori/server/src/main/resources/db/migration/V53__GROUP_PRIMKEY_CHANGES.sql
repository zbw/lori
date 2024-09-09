DROP TABLE group_right_map;
DROP TABLE right_group;
CREATE TABLE right_group(
    group_id serial primary key,
    title text,
    description text,
    ip_addresses json NOT NULL
);
create table group_right_map(
    group_id serial,
    right_id text,
    created_on timestamptz,
    created_by varchar(128),
    last_updated_on timestamptz,
    last_updated_by varchar(128),
    primary key (group_id, right_id),
    constraint group_id_fkey foreign key (group_id) REFERENCES right_group (group_id),
    constraint right_id_fkey foreign key (right_id) REFERENCES item_right (right_id)
);