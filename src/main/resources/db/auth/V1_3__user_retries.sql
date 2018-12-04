create table user_retries
(
  username    VARCHAR(30)          not null,
  retry_count INT                  not null DEFAULT 0,
  PRIMARY KEY (username)
);

