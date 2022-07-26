alter table item drop constraint right_id_fkey;
CREATE SEQUENCE item_right_right_id_seq START WITH 1;
alter table item_right alter column right_id set default nextval('item_right_right_id_seq');
alter table item alter column right_id set default nextval('item_right_right_id_seq');
alter table item add constraint right_id_fkey foreign key (right_id) REFERENCES item_right (right_id);
