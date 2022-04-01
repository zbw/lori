drop table item_restriction;
alter table item_action
    rename to item_right;

alter table item_right
    rename column action_id to right_id;
alter table item_metadata
    rename column header_id to metadata_id;

create table item(
    metadata_id varchar(255),
    right_id serial,
    primary key (metadata_id, right_id),
    constraint metadata_id_fkey foreign key (metadata_id) REFERENCES item_metadata (metadata_id),
    constraint right_id_fkey foreign key (right_id) REFERENCES item_right (right_id)
);

alter table item_right drop constraint access_right_header_header_id_fkey;
alter table item_right drop column header_id;
alter table item_right drop column permission;
alter table item_right drop column type;
alter table item_right add column access_state varchar(10);
alter table item_right add column start_date date;
alter table item_right add column end_date date;
alter table item_right add column license_conditions varchar(50);
alter table item_right add column provenance_license varchar(200);

alter table item_metadata drop column access_state;
alter table item_metadata drop column license_conditions;
alter table item_metadata drop column provenance_license;

alter table item drop constraint right_id_fkey;
alter table item_right alter column right_id type varchar(50);
alter table item alter column right_id type varchar(50);
alter table item add constraint right_id_fkey foreign key (right_id) REFERENCES item_right (right_id);
