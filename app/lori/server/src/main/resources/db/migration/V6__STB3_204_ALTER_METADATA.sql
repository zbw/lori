alter table item_metadata add column author varchar(255);
alter table item_metadata add column collection_name varchar(127);
alter table item_metadata add column community_name varchar(127);
alter table item_metadata add column storage_date timestamptz;