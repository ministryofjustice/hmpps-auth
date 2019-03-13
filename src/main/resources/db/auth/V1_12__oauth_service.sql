create table oauth_service
(
  code             varchar(64)  not null primary key,
  name             varchar(255) not null,
  description      varchar(255),
  authorised_roles varchar(1000),
  url              varchar(255) not null,
  enabled          bit          not null
);
