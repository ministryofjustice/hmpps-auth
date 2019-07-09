create table groups
(
    group_id        UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT group_pk PRIMARY KEY,
    group_code      VARCHAR(30)      NOT NULL
        CONSTRAINT group_code_uk UNIQUE,
    group_name      VARCHAR(100)     NOT NULL,
    create_datetime DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

create table user_email_groups
(
    groups_group_id    UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT user_group_group_fk REFERENCES groups (group_id),
    useremail_username VARCHAR(30)      NOT NULL
        CONSTRAINT user_group_user_email_fk REFERENCES user_email (username),
    create_datetime    DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX user_group_user_email_fk ON user_email_groups (useremail_username);
CREATE INDEX user_group_group_fk ON user_email_groups (groups_group_id);

ALTER TABLE authority
    ADD CONSTRAINT authority_pk PRIMARY KEY (authority_id);

CREATE INDEX authority_user_email_fk ON authority (username);
