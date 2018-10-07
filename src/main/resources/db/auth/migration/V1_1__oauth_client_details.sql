DROP TABLE IF EXISTS OAUTH_CLIENT_DETAILS;

create table OAUTH_CLIENT_DETAILS (
  client_id               varchar(64)  not null,
  access_token_validity   bigint,
  additional_information  varchar(255),
  authorities             varchar(255),
  authorized_grant_types  varchar(200) not null,
  autoapprove             varchar(200),
  client_secret           varchar(100) not null,
  refresh_token_validity  bigint,
  resource_ids            varchar(255),
  scope                   varchar(200),
  web_server_redirect_uri varchar(255),
  primary key (client_id)
);
