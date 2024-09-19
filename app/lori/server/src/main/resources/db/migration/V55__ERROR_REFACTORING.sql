DELETE FROM right_error;
ALTER TABLE right_error ADD COLUMN conflict_by_template_name TEXT NOT NULL DEFAULT '';
ALTER TABLE right_error RENAME COLUMN right_id_source TO conflict_by_right_id;
ALTER TABLE right_error ALTER COLUMN conflict_type SET NOT NULL;
ALTER TABLE right_error ALTER COLUMN created_on SET NOT NULL;
ALTER TABLE right_error ALTER COLUMN conflict_by_right_id SET NOT NULL;
ALTER TABLE right_error ALTER COLUMN metadata_id SET NOT NULL;
ALTER TABLE right_error ALTER COLUMN message SET NOT NULL;
ALTER TABLE right_error ALTER COLUMN handle_id SET NOT NULL;