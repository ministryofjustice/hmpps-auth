alter table user_email
  add token VARCHAR(240);
alter table user_email
  add token_type VARCHAR(10);

alter table user_email
  drop column verification_token;
alter table user_email
  drop column reset_token;

create index user_email_token_idx on user_email (token, token_type);
