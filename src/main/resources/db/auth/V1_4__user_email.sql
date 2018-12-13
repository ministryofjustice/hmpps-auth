create table user_email
(
  username           VARCHAR(30)  NOT NULL,
  email              VARCHAR(240) NOT NULL,
  create_datetime    DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verified           BIT          NOT NULL,
  verification_token VARCHAR(240),
  reset_token        VARCHAR(240),
  token_expiry       DATETIME2,
  PRIMARY KEY (username)
);

