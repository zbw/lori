ALTER TABLE group_right_map DROP CONSTRAINT group_id_fkey;
ALTER TABLE right_group ADD COLUMN version integer NOT NULL CONSTRAINT df_version DEFAULT (0);
ALTER TABLE right_group DROP CONSTRAINT right_group_pkey;
ALTER TABLE right_group ADD CONSTRAINT right_group_pkey PRIMARY KEY (group_id, version);