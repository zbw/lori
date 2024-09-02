CREATE INDEX template_name ON item_right USING btree(lower(template_name));
ALTER TABLE bookmark ADD COLUMN filter_series text;
ALTER TABLE bookmark ADD COLUMN filter_template_name text