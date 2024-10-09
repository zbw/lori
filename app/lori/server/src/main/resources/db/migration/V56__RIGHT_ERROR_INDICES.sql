CREATE INDEX conflict_type_idx ON right_error USING btree (conflict_type);
CREATE INDEX conflict_by_template_name_idx ON right_error USING btree (conflict_by_template_name);