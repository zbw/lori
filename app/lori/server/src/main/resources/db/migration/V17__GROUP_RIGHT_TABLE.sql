alter table right_group
    rename column name to group_id;
create table group_right_map(
    group_id text,
    right_id text,
    primary key (group_id, right_id),
    constraint group_id_fkey foreign key (group_id) REFERENCES right_group (group_id),
    constraint right_id_fkey foreign key (right_id) REFERENCES item_right (right_id)
);