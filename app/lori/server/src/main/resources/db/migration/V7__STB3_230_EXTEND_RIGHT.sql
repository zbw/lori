-- Drops
alter table item_right
    drop column license_conditions;
alter table item_right
    drop column provenance_license;

-- Group 0: Metadata of right information
alter table item_right
    add column notes_general text;
-- Group 1: Formal rules
alter table item_right
    add column licence_contract text;
alter table item_right
    add column author_right_exception bool;
alter table item_right
    add column zbw_user_agreement bool;
alter table item_right
    add column open_content_licence text;
alter table item_right
    add column non_standard_open_content_licence_url text;
alter table item_right
    add column non_standard_open_content_licence bool;
alter table item_right
    add column restricted_open_content_licence bool;
alter table item_right
    add column notes_formal_rules text;
-- Group 2: Process documentation
alter table item_right
    add column basis_storage varchar(50);
alter table item_right
    add column basis_access_state varchar(50);
alter table item_right
    add column notes_process_documentation text;
-- Group 3: Management related elements
alter table item_right
    add column notes_management_related text;
