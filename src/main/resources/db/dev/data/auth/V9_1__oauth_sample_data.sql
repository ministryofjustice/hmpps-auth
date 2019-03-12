INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('omicuser','1200',null,'SYSTEM_READ_ONLY','password,authorization_code,refresh_token','read','$2a$10$EiAoV/3ZSHl4KsVAkYZmH.pfZ6tLcK2vlvJTBgQBML3p3LrcPjaCi',null,null,'read','http://localhost:3000/login');
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('elite2apiclient','28800',null,null,'password,authorization_code,refresh_token','read,write','$2a$10$RYwV0QebHAovVXWPySb2lefr3HTDntGu1euXHDJc3zwh2NsqeNGHG','43200',null,'read,write','http://localhost:8081/login,http://localhost:3000/,http://localhost:3001/,http://localhost:3000/login/callback,http://localhost:3001/login/callback,http://localhost:3002/login/callback');
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('omic','28800',null,null,'password,authorization_code,refresh_token','read,write','$2a$10$oUonidUHlG34P/mbiRs2d.owes0fvNeyUBACo6lzkq7Hr/68cfxOW','43200',null,'read,write',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('licences','28800',null,null,'password,authorization_code,refresh_token','read,write','$2a$10$1FTv04xDqLuKWjBjBnxMJuQ9fEXH0CHJKZXpOjMB7hdmrMBoKhi7.','43200',null,'read,write','http://localhost:3000/login/callback,http://localhost:3000');
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('licencesadmin','3600',null,'ROLE_SYSTEM_USER,ROLE_GLOBAL_SEARCH,ROLE_LICENCE_RO','client_credentials','read,write','$2a$10$/JM78ghLrFNTWezv/rAoYe5Bv2HAHTtaQjzY44HTd2pHI82OxGiHy',null,null,'read,write',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('omicadmin','3600',null,'ROLE_MAINTAIN_ACCESS_ROLES,ROLE_SYSTEM_USER,ROLE_KW_MIGRATION,ROLE_KW_ADMIN','client_credentials','read','$2a$10$.95l4ENV1OEZ6qWd4R5QTOXZrjvTQmN402z1pjRUr2EwGFYdkDDnm',null,null,'read',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('batchadmin','3600',null,'ROLE_CONTACT_CREATE,ROLE_GLOBAL_SEARCH','client_credentials','read','$2a$10$UzbBEEyIFPTZGEle94.P5O.HyZ/46LxTByqC1sETfQKm8KVyO3k6O',null,null,'read',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('yjaftrustedclient','3600',null,'ROLE_GLOBAL_SEARCH,ROLE_BOOKING_CREATE,ROLE_BOOKING_RECALL','client_credentials','read','$2a$10$vVVSNBnu34VlNItT92f9QeW065zOyWBUX78fMZdzIOCPxyY1ETJuG',null,null,'read',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('delius','3600',null,'ROLE_SYSTEM_USER','client_credentials','read','$2a$10$wgC7niO2UpNykzZ4gsPcZOvKakPRwjGu.89C9AhQTCXsJG3JqTgK2',null,null,'read',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('apireporting','3600',null,'ROLE_REPORTING','client_credentials',null,'$2a$10$f93YXwvkwVx3mS1dsZzK/.dJzvm7gu7jHawG7xIUUJTYLtXkoQaNO',null,null,'reporting',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('custodyapi','28800',null,'ROLE_REPORTING','client_credentials',null,'$2a$10$ZClyyxwFbX/24Ab9KXflc.Id5cOv3qu4b1ryNkFmXzJZt9y8eJa82','43200',null,'reporting',null);-- 'password'
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('deliusnewtech','3600',null,'SYSTEM_READ_ONLY','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',null,null,'reporting',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('categorisationtool','3600',null,'ROLE_SYSTEM_USER','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',43200,null,'read',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('prisonstaffhubclient','3600',null,'ROLE_SYSTEM_READ_ONLY','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',43200,null,'read',null);
INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri) values ('risk-profiler','3600',null,'ROLE_SYSTEM_USER,ROLE_RISK_PROFILER','client_credentials',null,'$2a$10$r12DB/sqXduodnjtAY/ykO0S3KCySdVW4zhG3jlIRaIsfVkFOEds2',null,null,'read',null);

INSERT INTO user_retries (username, retry_count) VALUES ('LOCKED_USER', 5);

INSERT INTO user_email (username, email, verified)
VALUES ('LOCKED_USER', 'locked@somewhere.com', 'true'),
  ('CA_USER', 'ca_user@digital.justice.gov.uk', 'true'),
  ('ITAG_USER', 'itag_user@digital.justice.gov.uk', 'true'),
  ('ITAG_USER_ADM', 'itag_user_adm@digital.justice.gov.uk', 'true'),
  ('DM_USER', 'dm_user@digital.justice.gov.uk', 'false'),
  ('EXPIRED_TEST_USER', 'expired_test_user@digital.justice.gov.uk', 'true'),
  ('EXPIRED_TEST2_USER', 'expired_test2_user@digital.justice.gov.uk', 'true'),
  ('EXPIRED_TEST3_USER', 'expired_test3_user@digital.justice.gov.uk', 'true'),
  ('RO_DEMO', null, 'false'),
  ('CA_USER_TEST', 'ca_user@digital.justice.gov.uk', 'true'),
  ('RO_USER_TEST', 'ro_user_test@digital.justice.gov.uk', 'true'),
  ('DM_USER_TEST', 'dm_user_test@digital.justice.gov.uk', 'true');

INSERT INTO user_email (username, password, password_expiry, email, verified, enabled, locked, master)
VALUES ('AUTH_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_user@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
  ('AUTH_ADM', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_test2@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
  ('AUTH_TEST', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_test@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
  ('AUTH_NO_EMAIL', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', null, 'true', 'true', 'false', 'true'),
  ('AUTH_EXPIRED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2013-01-28 13:23:19', 'auth_test2@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
  ('AUTH_LOCKED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', null, 'true', 'true', 'true', 'true'),
  ('AUTH_LOCKED2', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_locked2@digital.justice.gov.uk', 'true', 'true', 'true', 'true'),
  ('AUTH_DISABLED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', null, 'true', 'false', 'false', 'true'),
  ('AUTH_RO_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_ro_user@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
  ('AUTH_RO_USER_TEST', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_ro_user_test@digital.justice.gov.uk', 'true', 'true', 'false', 'true');

INSERT INTO user_token (token, token_type, token_expiry, username)
VALUES ('reset', 'RESET', '2018-12-10 08:55:45', 'LOCKED_USER');

INSERT INTO person (username, first_name, last_name)
VALUES ('AUTH_USER', 'Auth', 'Only'),
  ('AUTH_ADM', 'Auth', 'Adm'),
  ('AUTH_TEST', 'Auth', 'Test'),
  ('AUTH_NO_EMAIL', 'Auth', 'NoEmail'),
  ('AUTH_EXPIRED', 'Auth', 'Expired'),
  ('AUTH_LOCKED', 'Auth', 'Locked'),
  ('AUTH_LOCKED2', 'Auth', 'Locked2'),
  ('AUTH_DISABLED', 'Auth', 'Disabled'),
  ('AUTH_RO_USER', 'Ryan-Auth', 'Orton'),
  ('AUTH_RO_USER_TEST', 'Ryan-Auth', 'Orton');


INSERT INTO authority (authority_id, username, authority)
VALUES ('36025454-e42d-49a1-9124-013577a7ed20', 'AUTH_ADM', 'ROLE_OAUTH_ADMIN'),
  ('a4843bf0-9b44-451c-ba3e-cdf04ba9eb3a', 'AUTH_ADM', 'ROLE_MAINTAIN_ACCESS_ROLES'),
  ('a4843bf0-9b44-451c-ba3e-cdf04ba9eb3b', 'AUTH_RO_USER', 'ROLE_LICENCE_RO'),
  ('a4843bf0-9b44-451c-ba3e-cdf04ba9eb3c', 'AUTH_RO_USER', 'ROLE_GLOBAL_SEARCH'),
  ('a4843bf0-9b44-451c-ba3e-cdf04ba9eb3d', 'AUTH_RO_USER_TEST', 'ROLE_LICENCE_RO'),
  ('a4843bf0-9b44-451c-ba3e-cdf04ba9eb3e', 'AUTH_RO_USER_TEST', 'ROLE_GLOBAL_SEARCH');
