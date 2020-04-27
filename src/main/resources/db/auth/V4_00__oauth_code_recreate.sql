DROP TABLE IF EXISTS oauth_code;

create table oauth_code
(
    code           VARCHAR(256) NOT NULL PRIMARY KEY,
    authentication VARBINARY( MAX),
    created_date   DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

