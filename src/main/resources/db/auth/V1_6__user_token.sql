create table user_token
(
  token           VARCHAR(240) NOT NULL,
  token_type      VARCHAR(10)  NOT NULL,
  username        VARCHAR(30)  NOT NULL,
  create_datetime DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  token_expiry    DATETIME2,
  PRIMARY KEY (token)
);

alter table user_email alter column email VARCHAR (240) NULL;
