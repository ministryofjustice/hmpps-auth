alter table user_email
  add password VARCHAR(100);
alter table user_email
  add enabled BIT NOT NULL DEFAULT 0;

alter table user_email
  drop column token_expiry;

create table authority
(
  authority_id UNIQUEIDENTIFIER NOT NULL,
  username     VARCHAR(30)      NOT NULL,
  authority    VARCHAR(50)      NOT NULL,
  constraint authorities_user_email_fk foreign key (username) references user_email (username)
);

create table person
(
  username   VARCHAR(30) NOT NULL,
  first_name VARCHAR(50) NOT NULL,
  last_name  VARCHAR(50) NOT NULL,
  constraint person_user_email_fk foreign key (username) references user_email (username),
  primary key (username)
);
