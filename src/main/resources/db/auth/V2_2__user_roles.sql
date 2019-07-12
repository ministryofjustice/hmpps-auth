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
VALUES (newid(), 'ROLE_GLOBAL_SEARCH', 'Global Search'),
       (newid(), 'ROLE_LICENCE_VARY', 'Licence Variation'),
       (newid(), 'ROLE_LICENCE_RO', 'Licence Responsible Officer'),
       (newid(), 'ROLE_PECS_POLICE', 'PECS Police'),
       (newid(), 'ROLE_PECS_SUPPLIER', 'PECS Supplier'),
       (newid(), 'ROLE_MAINTAIN_ACCESS_ROLES', 'Maintain Roles'),
       (newid(), 'ROLE_MAINTAIN_ACCESS_ROLES_ADMIN', 'Maintain Access Roles (admin)'),
       (newid(), 'ROLE_OAUTH_ADMIN', 'Auth Client Management (admin)'),
       (newid(), 'ROLE_MAINTAIN_OAUTH_USERS', 'Maintain Auth Users (admin)'),
       (newid(), 'ROLE_AUTH_GROUP_MANAGER', 'Auth Group Manager');

INSERT INTO user_email_roles (role_id, username) (select role_id, username
                                                  from authority a
                                                           join roles r on a.authority = r.role_code);


