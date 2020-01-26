create table roles
(
    role_id         UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT role_pk PRIMARY KEY,
    role_code       VARCHAR(50)      NOT NULL
        CONSTRAINT role_code_uk UNIQUE,
    role_name       VARCHAR(100)     NOT NULL,
    create_datetime DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

create table user_email_roles
(
    role_id         UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT user_role_role_fk REFERENCES roles (role_id),
    username        VARCHAR(30)      NOT NULL
        CONSTRAINT user_role_user_email_fk REFERENCES user_email (username),
    create_datetime DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX user_role_user_email_fk ON user_email_roles (username);
CREATE INDEX user_role_role_fk ON user_email_roles (role_id);

INSERT INTO roles (role_id, role_code, role_name)
VALUES (newid(), 'GLOBAL_SEARCH', 'Global Search'),
       (newid(), 'LICENCE_VARY', 'Licence Variation'),
       (newid(), 'LICENCE_RO', 'Licence Responsible Officer'),
       (newid(), 'NOMIS_BATCHLOAD', 'Licence Batch Load (admin)'),
       (newid(), 'PECS_POLICE', 'PECS Police'),
       (newid(), 'PECS_SUPPLIER', 'PECS Supplier'),
       (newid(), 'MAINTAIN_ACCESS_ROLES', 'Maintain Roles'),
       (newid(), 'MAINTAIN_ACCESS_ROLES_ADMIN', 'Maintain Access Roles (admin)'),
       (newid(), 'OAUTH_ADMIN', 'Auth Client Management (admin)'),
       (newid(), 'MAINTAIN_OAUTH_USERS', 'Maintain Auth Users (admin)'),
       (newid(), 'AUTH_GROUP_MANAGER', 'Auth Group Manager');

INSERT INTO user_email_roles (role_id, username)
    (SELECT role_id, username
     FROM authority a
              JOIN roles r ON a.authority = concat('ROLE_', r.role_code));


