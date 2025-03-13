ALTER TABLE item_metadata
    ALTER COLUMN isbn type text[] using NULLIF(ARRAY[isbn], '{null}');