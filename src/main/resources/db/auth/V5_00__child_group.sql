create table child_group
(
    child_group_id   UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT child_group_pk PRIMARY KEY,
    child_group_code VARCHAR(30)      NOT NULL
        CONSTRAINT child_group_code_uk UNIQUE,
    child_group_name VARCHAR(100)     NOT NULL,
    group_id         UNIQUEIDENTIFIER NOT NULL
);
