alter table access_right_header
    rename to item_metadata;
alter table access_right_action
    rename to item_action;
alter table access_right_restriction
    rename to item_restriction;
alter table item_metadata
drop
column tenant;
alter table item_metadata
drop
column usage_guide;
alter table item_metadata
drop
column template;
alter table item_metadata
drop
column mention;
alter table item_metadata
drop
column sharealike;
alter table item_metadata
drop
column commercial_use;
alter table item_metadata
drop
column copyright;
alter table item_metadata
    add handle varchar(255) NOT NULL;
alter table item_metadata
    add ppn varchar(255);
alter table item_metadata
    add ppn_ebook varchar(255);
alter table item_metadata
    add title varchar(512) NOT NULL;
alter table item_metadata
    add title_journal varchar(255);
alter table item_metadata
    add title_series varchar(255);
alter table item_metadata
    add access_state varchar(32);
alter table item_metadata
    add published_year integer NOT NULL;
alter table item_metadata
    add band varchar(255);
alter table item_metadata
    add publication_type varchar(100) NOT NULL;
alter table item_metadata
    add doi varchar(255);
alter table item_metadata
    add serial_number varchar(100);
alter table item_metadata
    add isbn varchar(20);
alter table item_metadata
    add rights_k10plus varchar(100);
alter table item_metadata
    add paket_sigel varchar(100);
alter table item_metadata
    add zbd_id varchar(64);
alter table item_metadata
    add issn varchar(32);

create index item_handle_idx on item_metadata (handle);
create index item_doi_idx on item_metadata (doi);
create index item_isbn_idx on item_metadata (isbn);
create index item_issn_idx on item_metadata (issn);
create index item_title_idx on item_metadata (title);