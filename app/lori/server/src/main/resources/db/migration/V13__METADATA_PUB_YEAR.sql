-- Drop unused columns
alter table item_metadata drop column ppn_ebook;
alter table item_metadata drop column serial_number;

-- Change name and type of published_year variable. It's more a date than only the year.
alter table item_metadata drop column published_year;
alter table item_metadata add column publication_date date;
