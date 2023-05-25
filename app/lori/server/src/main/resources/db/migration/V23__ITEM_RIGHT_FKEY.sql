alter table item_right add column template_id int default null;
alter table item_right add constraint template_id_fkey foreign key (template_id) REFERENCES template (template_id);