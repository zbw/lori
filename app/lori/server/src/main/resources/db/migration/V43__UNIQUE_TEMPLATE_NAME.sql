alter table item_right add unique (template_name);
alter table right_error drop column template_id_source;