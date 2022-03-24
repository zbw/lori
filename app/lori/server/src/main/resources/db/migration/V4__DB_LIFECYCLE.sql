alter table item_metadata
    add created_on timestamptz;
alter table item_metadata
    add last_updated_on timestamptz;
alter table item_metadata
    add created_by varchar(128);
alter table item_metadata
    add last_updated_by varchar(128);

alter table item_action
    add created_on timestamptz;
alter table item_action
    add last_updated_on timestamptz;
alter table item_action
    add created_by varchar(128);
alter table item_action
    add last_updated_by varchar(128);