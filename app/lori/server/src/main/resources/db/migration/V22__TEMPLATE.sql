create table template(
    template_id serial primary key,
    template_name text not null,
    template_description text,
    right_id varchar(50) not null,
    constraint right_id_fkey foreign key (right_id) REFERENCES item_right (right_id)
);

create table template_bookmark_map(
    template_id serial,
    bookmark_id serial,
    constraint template_id_fkey foreign key (template_id) REFERENCES template (template_id),
    constraint bookmark_id_fkey foreign key (bookmark_id) REFERENCES bookmark (bookmark_id)
);
