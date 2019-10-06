CREATE TABLE user_group
(
    group_id        UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT user_group_group_id_fk REFERENCES groups (group_id),
    user_id         UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT user_group_user_id_fk REFERENCES users (user_id),
    create_datetime DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX user_group_user_id_fk ON user_group (user_id);
CREATE INDEX user_group_group_id_fk ON user_group (group_id);

-- copy data over to new table
INSERT INTO user_group (group_id, user_id, create_datetime)
SELECT groups_group_id, user_id, ueg.create_datetime
FROM user_email_groups ueg
         JOIN users u ON ueg.useremail_username = u.username
