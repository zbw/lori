alter table item_right add first_applied_on timestamptz;
UPDATE item_right
    SET first_applied_on = start_date::timestamptz AT TIME ZONE 'UTC'
    WHERE last_applied_on IS NOT NULL;
