ALTER TABLE item_right
    ADD COLUMN has_exception_id text;

UPDATE item_right t_parent
SET has_exception_id = t_child.right_id
FROM item_right t_child
WHERE t_child.exception_from = t_parent.right_id;

ALTER TABLE item_right RENAME exception_from TO exception_of_id;