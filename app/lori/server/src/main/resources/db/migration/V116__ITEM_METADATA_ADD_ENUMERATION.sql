ALTER TABLE item_metadata ADD COLUMN IF NOT EXISTS enumeration TEXT;

-- Create trigger function
CREATE OR REPLACE FUNCTION set_column_enumeration()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.econstor_volume IS NULL OR NEW.econstor_volume = '' THEN
        NEW.enumeration := NEW.econstor_issue;
    ELSIF NEW.econstor_issue IS NULL OR NEW.econstor_issue = '' THEN
        NEW.enumeration := NEW.econstor_volume;
    ELSE
        NEW.enumeration := NEW.econstor_volume || ',' || NEW.econstor_issue;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Backfill existing records
UPDATE item_metadata
SET enumeration =
        CASE
            WHEN econstor_volume IS NULL OR econstor_volume = '' THEN econstor_issue
            WHEN econstor_issue IS NULL OR econstor_issue = '' THEN econstor_volume
            ELSE econstor_volume || ',' || econstor_issue
            END;

-- Create triggers on insert and updates
CREATE TRIGGER trigger_set_column_enumeration
    BEFORE INSERT ON item_metadata
    FOR EACH ROW
EXECUTE FUNCTION set_column_enumeration();

CREATE TRIGGER trigger_update_column_enumeration
    BEFORE UPDATE ON item_metadata
    FOR EACH ROW
EXECUTE FUNCTION set_column_enumeration();

-- Add the index for sorting
CREATE INDEX idx_item_metadata_enumeration ON item_metadata (enumeration);