ALTER TABLE item_metadata
    ADD COLUMN IF NOT EXISTS is_part_of_series_joined_lower text;

DO $$
    DECLARE
        batch_size INT := 5000;
        updated_count INT;
    BEGIN
        LOOP
            WITH cte AS (
                SELECT handle
                FROM item_metadata
                WHERE is_part_of_series IS NOT NULL
                  AND (
                    is_part_of_series_joined_lower IS NULL
                        OR is_part_of_series_joined_lower IS DISTINCT FROM lower(array_to_string(is_part_of_series, ' '))
                    )
                    FOR UPDATE SKIP LOCKED
                LIMIT batch_size
            )
            UPDATE item_metadata im
            SET is_part_of_series_joined_lower = lower(array_to_string(im.is_part_of_series, ' '))
            FROM cte
            WHERE im.handle = cte.handle;

            -- get number of updated rows (no RETURNING rows!)
            GET DIAGNOSTICS updated_count = ROW_COUNT;

            IF updated_count = 0 THEN
                EXIT;
            END IF;
        END LOOP;
    END $$;

-- Create trigger function (idempotent)
CREATE OR REPLACE FUNCTION trg_update_is_part_of_series_joined_lower()
    RETURNS trigger AS $$
BEGIN
    IF NEW.is_part_of_series IS NOT NULL THEN
        NEW.is_part_of_series_joined_lower := lower(array_to_string(NEW.is_part_of_series, ' '));
    ELSE
        NEW.is_part_of_series_joined_lower := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql STRICT;

-- Drop existing trigger if any (harmless) and create the trigger
DROP TRIGGER IF EXISTS trg_is_part_of_series_joined_lower ON item_metadata;

CREATE TRIGGER trg_is_part_of_series_joined_lower
    BEFORE INSERT OR UPDATE OF is_part_of_series
    ON item_metadata
    FOR EACH ROW
EXECUTE FUNCTION trg_update_is_part_of_series_joined_lower();

-- Build trigram GIN index non-transactionally to avoid locks
CREATE INDEX IF NOT EXISTS is_part_of_series_joined_lower_trgm_idx
    ON item_metadata USING GIN (is_part_of_series_joined_lower gin_trgm_ops);
