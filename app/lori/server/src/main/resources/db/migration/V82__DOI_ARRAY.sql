ALTER TABLE item_metadata
    ALTER COLUMN doi type text[] using NULLIF(ARRAY[doi], '{null}');