alter table template
    add created_on timestamptz;
alter table template
    add created_by varchar(128);

alter table template
    add last_updated_on timestamptz;
alter table template
    add last_updated_by varchar(128);

alter table template
    add last_applied_on timestamptz;