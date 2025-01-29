alter table item_metadata
    alter column publication_date type integer
        using EXTRACT(YEAR FROM publication_date);