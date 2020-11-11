ALTER TABLE child_group
    ADD CONSTRAINT child_group_group_id_fk FOREIGN KEY (group_id) REFERENCES groups (group_id);
