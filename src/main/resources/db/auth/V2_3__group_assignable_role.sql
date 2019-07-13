create table group_assignable_role
(
    role_id         UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT group_assignable_role_role_fk REFERENCES roles (role_id),
    group_id        UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT group_assignable_role_group_fk REFERENCES groups (group_id),
    automatic       BIT              NOT NULL,
    create_datetime DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX group_assignable_role_role_fk ON group_assignable_role (role_id);
CREATE INDEX group_assignable_role_group_fk ON group_assignable_role (group_id);
