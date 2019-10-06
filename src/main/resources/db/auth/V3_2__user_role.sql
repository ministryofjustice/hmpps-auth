CREATE TABLE user_role
(
    role_id         UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT user_role_role_id_fk REFERENCES roles (role_id),
    user_id         UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT user_role_user_id_fk REFERENCES users (user_id),
    create_datetime DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX user_role_user_id_fk ON user_role (user_id);
CREATE INDEX user_role_role_id_fk ON user_role (role_id);

-- copy data over to new table
INSERT INTO user_role (role_id, user_id, create_datetime)
SELECT role_id, user_id, uer.create_datetime
FROM user_email_roles uer
         JOIN users u ON uer.username = u.username
