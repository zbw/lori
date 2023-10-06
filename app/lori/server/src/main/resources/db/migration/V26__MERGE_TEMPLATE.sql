-- Drop foreign key in item_right
alter table item_right drop constraint template_id_fkey;
alter table item_right drop column template_id;
-- Add template columns to item_right
alter table item_right add last_applied_on timestamptz;
alter table item_right add template_id integer unique;
alter table item_right add template_name text;
alter table item_right add template_description text;


-- Drop constraint from bookmark mapping
alter table template_bookmark_map drop constraint template_id_fkey;

-- Add constraint to bookmark mapping
alter table template_bookmark_map add constraint template_id_fkey foreign key (template_id) REFERENCES item_right (template_id);

-- Drop template table
drop table template;