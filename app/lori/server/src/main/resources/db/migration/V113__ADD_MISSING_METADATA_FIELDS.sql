ALTER TABLE item_metadata DROP COLUMN author;
ALTER TABLE item_metadata DROP COLUMN issn;
ALTER TABLE item_metadata DROP COLUMN band;
ALTER TABLE item_metadata DROP COLUMN title_journal;
ALTER TABLE item_metadata DROP COLUMN title_series;
ALTER TABLE item_metadata ADD COLUMN issn text[];
ALTER TABLE item_metadata
    ADD COLUMN IF NOT EXISTS issn_joined_lower text;

CREATE OR REPLACE FUNCTION trg_update_issn_joined_lower()
    RETURNS trigger AS $$
BEGIN
    IF NEW.issn IS NOT NULL THEN
        NEW.issn_joined_lower := lower(array_to_string(NEW.issn, ' '));
    ELSE
        NEW.issn_joined_lower := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql STRICT;

CREATE TRIGGER trg_issn_joined_lower
    BEFORE INSERT OR UPDATE OF issn
    ON item_metadata
    FOR EACH ROW
EXECUTE FUNCTION trg_update_issn_joined_lower();

-- Build trigram GIN index non-transactionally to avoid locks
CREATE INDEX IF NOT EXISTS issn_joined_lower_trgm_idx
    ON item_metadata USING GIN (issn_joined_lower gin_trgm_ops);

ALTER TABLE item_metadata ADD COLUMN is_part_of_journal text;
CREATE INDEX is_part_of_journal_idx ON item_metadata (LOWER(is_part_of_journal));

ALTER TABLE item_metadata ADD COLUMN is_part_of_book text;
CREATE INDEX is_part_of_book_idx ON item_metadata (LOWER(is_part_of_book));

ALTER TABLE item_metadata ADD COLUMN ppn_book text;
CREATE INDEX ppn_book_lower_idx ON item_metadata (LOWER(ppn_book));

ALTER TABLE item_metadata ADD COLUMN ppn_journal text;
CREATE INDEX ppn_journal_lower_idx ON item_metadata (LOWER(ppn_journal));

ALTER TABLE item_metadata ADD COLUMN ppn_series text;
CREATE INDEX ppn_series_lower_idx ON item_metadata (LOWER(ppn_series));

ALTER TABLE item_metadata ADD COLUMN econstor_volume text;
CREATE INDEX econstor_volume_idx ON item_metadata (LOWER(item_metadata.econstor_volume));

ALTER TABLE item_metadata ADD COLUMN econstor_issue text;
CREATE INDEX econstor_issue_idx ON item_metadata (LOWER(item_metadata.econstor_issue));


