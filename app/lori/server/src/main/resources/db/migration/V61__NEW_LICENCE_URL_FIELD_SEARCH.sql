DROP INDEX ts_licence_url_idx;
ALTER TABLE item_metadata ADD COLUMN licence_url_filter text;
CREATE INDEX licence_url_filter_idx ON item_metadata USING btree(licence_url_filter);
ALTER TABLE bookmark ADD COLUMN filter_licence_url text;