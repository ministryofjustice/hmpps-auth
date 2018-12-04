create table user_retries
(
  username    VARCHAR2(30)          not null,
  retry_count NUMBER(3,0) DEFAULT 0 not null,
  PRIMARY KEY (username)
);

