create table groups
(
    group_id        UNIQUEIDENTIFIER NOT NULL,
    group_code      VARCHAR(30)      NOT NULL,
    group_name      VARCHAR(100)     NOT NULL,
    create_datetime DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT group_code_uk UNIQUE (group_code)
);

--CREATE UNIQUE INDEX group_code_uk ON groups (group_code);

create table user_email_groups
(
    groups_group_id    UNIQUEIDENTIFIER NOT NULL,
    useremail_username VARCHAR(30)      NOT NULL,
    create_datetime    DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_group_user_email_fk FOREIGN KEY (useremail_username) REFERENCES user_email (username),
    CONSTRAINT user_group_group_fk FOREIGN KEY (groups_group_id) REFERENCES groups (group_id)
);


