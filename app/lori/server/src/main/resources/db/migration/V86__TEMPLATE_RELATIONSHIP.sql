ALTER TABLE item_right
    ADD COLUMN predecessor_id text,
    ADD CONSTRAINT predecessor_id_fkey
        FOREIGN KEY (predecessor_id)
            REFERENCES item_right (right_id)
            ON DELETE SET NULL;

ALTER TABLE item_right
    ADD COLUMN successor_id text,
    ADD CONSTRAINT successor_id_fkey
        FOREIGN KEY (successor_id)
            REFERENCES item_right (right_id)
            ON DELETE SET NULL;
