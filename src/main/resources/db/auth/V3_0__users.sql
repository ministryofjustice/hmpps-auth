create table users
(
    user_id         UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT user_id_pk PRIMARY KEY,
    username        VARCHAR(30)      NOT NULL
        CONSTRAINT username_uk UNIQUE,
    password        VARCHAR(100),
    email           VARCHAR(240),
    first_name      VARCHAR(50),
    last_name       VARCHAR(50),
    verified        BIT              NOT NULL DEFAULT 0,
    locked          BIT              NOT NULL DEFAULT 0,
    enabled         BIT              NOT NULL DEFAULT 0,
    master          BIT              NOT NULL DEFAULT 0,
    create_datetime DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    password_expiry DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_logged_in  DATETIME2        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX user_email_idx ON users (email);
CREATE INDEX user_last_logged_in_enabled_idx ON users (last_logged_in, enabled);

-- copy data over to new table
INSERT INTO users (user_id,
                   username,
                   password,
                   email,
                   first_name,
                   last_name,
                   verified,
                   locked,
                   enabled,
                   master,
                   create_datetime,
                   password_expiry,
                   last_logged_in)
SELECT newid(),
       ue.username,
       password,
       email,
       first_name,
       last_name,
       verified,
       locked,
       enabled,
       master,
       create_datetime,
       password_expiry,
       last_logged_in
FROM user_email ue
         LEFT OUTER JOIN person p ON ue.username = p.username;
