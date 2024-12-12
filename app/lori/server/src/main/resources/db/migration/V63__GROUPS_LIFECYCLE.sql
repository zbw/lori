ALTER TABLE right_group ADD COLUMN created_by text;
ALTER TABLE right_group ADD COLUMN created_on timestamptz;
ALTER TABLE right_group ADD COLUMN last_updated_by text;
ALTER TABLE right_group ADD COLUMN last_updated_on timestamptz;