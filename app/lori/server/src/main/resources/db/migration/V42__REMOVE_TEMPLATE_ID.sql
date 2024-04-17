drop table template_bookmark_map;
create table template_bookmark_map
(
    right_id text,
    bookmark_id serial,
    primary key (bookmark_id, right_id),
    constraint right_id_fkey foreign key (right_id) REFERENCES item_right (right_id),
    constraint bookmark_id_fkey foreign key (bookmark_id) REFERENCES bookmark (bookmark_id)
);
alter table item_right add column is_template bool not null default false;
update item_right set is_template = true where template_id is not null;
alter table item_right drop column template_id;