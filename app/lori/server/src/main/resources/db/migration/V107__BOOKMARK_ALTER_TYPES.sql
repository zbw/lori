-- 1. Alter the column to text[]
ALTER TABLE bookmark
    ALTER COLUMN filter_paket_sigel TYPE text[]
        USING (ARRAY[filter_paket_sigel]);

-- 2. Update the column to convert single text values into an array with one entry
UPDATE bookmark
SET filter_paket_sigel = CASE
                   WHEN filter_paket_sigel IS NOT NULL THEN ARRAY[filter_paket_sigel]
                   ELSE NULL
    END;

-- 1. Alter the column to text[]
ALTER TABLE bookmark
    ALTER COLUMN filter_zdb_id TYPE text[]
        USING (ARRAY[filter_zdb_id]);

-- 2. Update the column to convert single text values into an array with one entry
UPDATE bookmark
SET filter_zdb_id = CASE
                   WHEN filter_zdb_id IS NOT NULL THEN ARRAY[filter_zdb_id]
                   ELSE NULL
    END;

