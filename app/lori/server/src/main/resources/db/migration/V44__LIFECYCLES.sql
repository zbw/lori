alter table bookmark
    add created_on timestamptz;
alter table bookmark
    add created_by varchar(128);
alter table bookmark
    add last_updated_on timestamptz;
alter table bookmark
    add last_updated_by varchar(128);

alter table group_right_map
    add created_on timestamptz;
alter table group_right_map
    add created_by varchar(128);
alter table group_right_map
    add last_updated_on timestamptz;
alter table group_right_map
    add last_updated_by varchar(128);

alter table item
    add created_on timestamptz;
alter table item
    add created_by varchar(128);
alter table item
    add last_updated_on timestamptz;
alter table item
    add last_updated_by varchar(128);

alter table template_bookmark_map
    add created_on timestamptz;
alter table template_bookmark_map
    add created_by varchar(128);
alter table template_bookmark_map
    add last_updated_on timestamptz;
alter table template_bookmark_map
    add last_updated_by varchar(128);

alter table sessions
    add created_on timestamptz;

alter table right_group
    add created_on timestamptz;
alter table right_group
    add created_by varchar(128);
alter table right_group
    add last_updated_on timestamptz;
alter table right_group
    add last_updated_by varchar(128);
