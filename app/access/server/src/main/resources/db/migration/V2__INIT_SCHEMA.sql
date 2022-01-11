alter table access_right_header
    rename to item;
alter table access_right_action
    rename to item_action;
alter table access_right_restriction
    rename to item_restriction;
alter table item
drop
column tenant;
alter table item
drop
column usage_guide;
alter table item
drop
column template;
alter table item
drop
column mention;
alter table item
drop
column sharealike;
alter table item
drop
column commercial_use;
alter table item
drop
column copyright;
alter table item
    add handle varchar(255);
alter table item
    add ppn varchar(255);
alter table item
    add ppn_ebook varchar(255);
alter table item
    add title varchar(512);
alter table item
    add title_journal varchar(255);
alter table item
    add title_series varchar(255);
alter table item
    add access_state varchar(32);
alter table item
    add publishedYear integer;
alter table item
    add band varchar(255);
alter table item
    add publicationtype varchar(100);
alter table item
    add doi varchar(255);
alter table item
    add serialNumber varchar(100);
alter table item
    add isbn varchar(20);
alter table item
    add rights_k10plus varchar(100);
alter table item
    add paket_sigel varchar(100);
alter table item
    add zbd_id varchar(64);
alter table item
    add issn varchar(32);