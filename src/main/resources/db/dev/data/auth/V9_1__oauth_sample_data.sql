INSERT INTO oauth_client_details (client_id, access_token_validity, additional_information, authorities, authorized_grant_types, autoapprove, client_secret, refresh_token_validity, resource_ids, scope, web_server_redirect_uri)
VALUES ('omicuser','1200',null,'SYSTEM_READ_ONLY','password,authorization_code,refresh_token','read','$2a$10$EiAoV/3ZSHl4KsVAkYZmH.pfZ6tLcK2vlvJTBgQBML3p3LrcPjaCi',null,null,'read','http://localhost:3000/login'),
       ('elite2apiclient','28800',null,null,'password,authorization_code,refresh_token','read,write','$2a$10$RYwV0QebHAovVXWPySb2lefr3HTDntGu1euXHDJc3zwh2NsqeNGHG','43200',null,'read,write','http://localhost:8081/login,http://localhost:3000/,http://localhost:3001/,http://localhost:3000/login/callback,http://localhost:3001/login/callback,http://localhost:3002/login/callback,http://localhost:8081/webjars/springfox-swagger-ui/oauth2-redirect.html'),
       ('omic','28800',null,null,'password,authorization_code,refresh_token','read,write','$2a$10$oUonidUHlG34P/mbiRs2d.owes0fvNeyUBACo6lzkq7Hr/68cfxOW','43200',null,'read,write',null),
       ('licences','28800',null,null,'password,authorization_code,refresh_token','read,write','$2a$10$1FTv04xDqLuKWjBjBnxMJuQ9fEXH0CHJKZXpOjMB7hdmrMBoKhi7.','43200',null,'read,write','http://localhost:3000/login/callback,http://localhost:3000'),
       ('licencesadmin','3600',null,'ROLE_SYSTEM_USER,ROLE_GLOBAL_SEARCH,ROLE_LICENCE_RO','client_credentials','read,write','$2a$10$/JM78ghLrFNTWezv/rAoYe5Bv2HAHTtaQjzY44HTd2pHI82OxGiHy',null,null,'read,write',null),
       ('omicadmin','3600',null,'ROLE_MAINTAIN_ACCESS_ROLES,ROLE_SYSTEM_USER,ROLE_KW_MIGRATION,ROLE_KW_ADMIN','client_credentials','read','$2a$10$.95l4ENV1OEZ6qWd4R5QTOXZrjvTQmN402z1pjRUr2EwGFYdkDDnm',null,null,'read',null),
       ('batchadmin','3600',null,'ROLE_CONTACT_CREATE,ROLE_GLOBAL_SEARCH','client_credentials','read','$2a$10$UzbBEEyIFPTZGEle94.P5O.HyZ/46LxTByqC1sETfQKm8KVyO3k6O',null,null,'read',null),
       ('yjaftrustedclient','3600',null,'ROLE_GLOBAL_SEARCH,ROLE_BOOKING_CREATE,ROLE_BOOKING_RECALL','client_credentials','read','$2a$10$vVVSNBnu34VlNItT92f9QeW065zOyWBUX78fMZdzIOCPxyY1ETJuG',null,null,'read',null),
       ('delius','3600',null,'ROLE_SYSTEM_USER','client_credentials','read','$2a$10$wgC7niO2UpNykzZ4gsPcZOvKakPRwjGu.89C9AhQTCXsJG3JqTgK2',null,null,'read',null),
       ('apireporting','3600',null,'ROLE_REPORTING','client_credentials',null,'$2a$10$f93YXwvkwVx3mS1dsZzK/.dJzvm7gu7jHawG7xIUUJTYLtXkoQaNO',null,null,'reporting',null),
       ('custodyapi','28800',null,'ROLE_REPORTING','client_credentials',null,'$2a$10$ZClyyxwFbX/24Ab9KXflc.Id5cOv3qu4b1ryNkFmXzJZt9y8eJa82','43200',null,'reporting',null),-- 'password'
       ('deliusnewtech','3600',null,'SYSTEM_READ_ONLY','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',null,null,'reporting',null),
       ('categorisationtool','3600',null,'ROLE_RISK_PROFILER','password,authorization_code,refresh_token,client_credentials','read,write' ,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',43200,null,'read,write','http://localhost:3000/login/callback,http://localhost:3000'),
       ('prisonstaffhubclient','3600',null,'ROLE_SYSTEM_READ_ONLY','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',43200,null,'read',null),
       ('risk-profiler','3600',null,'ROLE_SYSTEM_USER,ROLE_RISK_PROFILER','client_credentials',null,'$2a$10$r12DB/sqXduodnjtAY/ykO0S3KCySdVW4zhG3jlIRaIsfVkFOEds2',null,null,'read',null),
       ('sentence-plan-client','3600',null,null,'authorization_code,refresh_token','read,write','$2a$10$a5WJN/AZc7Nq3rFoy5GOQ.avY.opPq/RaF59TXFaInt0Jxp6NV94a',43200,null,'read,write','http://localhost:3000/login/callback,http://localhost:3000'),
       ('use-of-force-client','3600',null,null,'authorization_code,refresh_token','read,write','$2a$10$YRkR9FGSpZu3FAn5.Awtk.Yd0hg92y63VfVVAKhS6k66nMsc3/Hiy',43200,null,'read,write','http://localhost:3000/login/callback,http://localhost:3000'),
       ('whereabouts-api-client','3600',null,'ROLE_PAY, ROLE_CASE_NOTE_ADMIN','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',null,null,'read,write',null),
       ('pathfinder-client','3600',null,null,'authorization_code,refresh_token','read,write','$2a$10$WzgtydqXSuhdivpWDR3WXO.yjLBm4yuDqP64Og.7E4XURdrSfhOTi',43200,null,'read,write','http://localhost:3000/login/callback,http://localhost:3000');


INSERT INTO oauth_service (code, name, description, authorised_roles, url, enabled, email)
VALUES ('BOOK_MOVE', 'Book a secure move', 'Book a secure move', 'ROLE_PECS_SUPPLIER,ROLE_PECS_POLICE,', 'https://bookasecuremove.service.justice.gov.uk', 1, 'bookasecuremove@digital.justice.gov.uk'),
       ('CATTOOL', 'Digital Categorisation Service', 'Service to support categorisation of prisoners providing a consistent workflow and risk indicators.', 'ROLE_CREATE_CATEGORISATION,ROLE_APPROVE_CATEGORISATION,ROLE_CATEGORISATION_SECURITY,ROLE_CREATE_RECATEGORISATION', 'https://offender-categorisation.service.justice.gov.uk', 1, 'categorisation@justice.gov.uk'),
       ('HDC', 'Home Detention Curfew', 'Service for HDC Licences Creation and Approval', 'ROLE_LICENCE_CA,ROLE_LICENCE_RO,ROLE_LICENCE_DM', 'http://localhost:3003', 1, 'hdcdigitalservice@digital.justice.gov.uk'),
       ('KW', 'Keyworker Management Service', 'Service to allow viewing and allocation of Key workers to prisoners and viewing of prison and staff level statistics.', 'ROLE_OMIC_ADMIN,ROLE_KEYWORKER_MONITOR', 'http://localhost:3001/manage-key-workers', 1, null),
       ('NOMIS', 'Digital Prison Service', 'View and Manage Offenders in Prison (Old name was NEW NOMIS)', null, 'http://localhost:3000', 1, 'feedback@digital.justice.gov.uk'),
       ('OAUTHADMIN', 'Oauth Client Management', 'Manage Client Credentials for OAUTH2 Clients', 'ROLE_OAUTH_ADMIN', 'http://localhost:8080/auth/ui/', 1, null),
       ('POM', 'Allocate a POM Service', 'Allocate the appropriate offender manager to a prisoner', 'ROLE_ALLOC_MGR', 'https://moic.service.justice.gov.uk', 1, 'moic@digital.justice.gov.uk'),
       ('USERADMIN', 'Admin & Utilities Service', 'Admin & utilities Service For NOMIS and Auth User', 'ROLE_KW_MIGRATION,ROLE_MAINTAIN_ACCESS_ROLES,ROLE_MAINTAIN_ACCESS_ROLES_ADMIN,ROLE_MAINTAIN_OAUTH_USERS,ROLE_AUTH_GROUP_MANAGER', 'http://localhost:3001/admin-utilities', 1, null);


INSERT INTO user_retries (username, retry_count)
VALUES ('LOCKED_USER', 5),
       ('AUTH_DELETEALL', 3),
       ('NOMIS_DELETE', 1);


-- nomis users
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
       ('CA_USER_TEST', 'reset_test@digital.justice.gov.uk', 'true'),
       ('RO_USER_TEST', 'ro_user_test@digital.justice.gov.uk', 'true'),
       ('DM_USER_TEST', 'dm_user_test@digital.justice.gov.uk', 'true'),
       ('RESET_TEST_USER', 'reset_test@digital.justice.gov.uk', 'true'),
       ('PPL_USER', 'ppl_user@digital.justice.gov.uk', 'true');

INSERT INTO user_email (username, email, verified, last_logged_in)
VALUES ('NOMIS_DELETE', 'locked@somewhere.com', 'true', '2018-02-04 13:23:19');

-- auth users
INSERT INTO user_email (username, password, password_expiry, email, verified, enabled, locked, master)
VALUES ('AUTH_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_user@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
       ('AUTH_ADM', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_test2@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
       ('AUTH_TEST', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_test@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
       ('AUTH_NO_EMAIL', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', null, 'true', 'true', 'false', 'true'),
       ('AUTH_EXPIRED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2013-01-28 13:23:19', 'auth_test2@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
       ('AUTH_LOCKED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', null, 'true', 'true', 'true', 'true'),
       ('AUTH_LOCKED2', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_locked2@digital.justice.gov.uk', 'true', 'true', 'true', 'true'),
       ('AUTH_DISABLED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', null, 'true', 'false', 'false', 'true'),
       ('AUTH_STATUS', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', null, 'true', 'false', 'false', 'true'),
       ('AUTH_NEW_USER', null, '3013-01-28 13:23:19', 'a@b.com', 'false', 'true', 'false', 'true'),
       ('AUTH_RO_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_ro_user@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
       ('AUTH_RO_VARY_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_ro_user@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
       ('AUTH_GROUP_MANAGER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_group_manager@digital.justice.gov.uk', 'true', 'true', 'false', 'true'),
       ('AUTH_RO_USER_TEST', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19', 'auth_ro_user_test@digital.justice.gov.uk', 'true', 'true', 'false', 'true');

INSERT INTO user_email (username, password, last_logged_in, verified, enabled, locked, master)
VALUES ('AUTH_INACTIVE', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2019-02-03 13:23:19', 'true', 'true', 'false', 'true'),
       ('AUTH_DELETE', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2018-01-02 13:23:19', 'true', 'false', 'false', 'true'),
       ('AUTH_DELETEALL', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2018-02-03 13:23:19', 'true', 'false', 'false', 'true');

INSERT INTO user_token (token, token_type, token_expiry, username)
VALUES ('reset', 'RESET', '2018-12-10 08:55:45', 'LOCKED_USER'),
       ('reset2', 'RESET', '2018-12-10 08:55:45', 'AUTH_DELETEALL'),
       ('verified', 'VERIFIED', '2018-12-10 08:55:45', 'AUTH_DELETEALL');

INSERT INTO person (username, first_name, last_name)
VALUES ('AUTH_USER', 'Auth', 'Only'),
       ('AUTH_ADM', 'Auth', 'Adm'),
       ('AUTH_TEST', 'Auth', 'Test'),
       ('AUTH_NO_EMAIL', 'Auth', 'NoEmail'),
       ('AUTH_EXPIRED', 'Auth', 'Expired'),
       ('AUTH_LOCKED', 'Auth', 'Locked'),
       ('AUTH_LOCKED2', 'Auth', 'Locked2'),
       ('AUTH_DISABLED', 'Auth', 'Disabled'),
       ('AUTH_DELETE', 'Auth', 'Delete'),
       ('AUTH_DELETEALL', 'Auth', 'DeleteAll'),
       ('AUTH_INACTIVE', 'Auth', 'Inactive'),
       ('AUTH_STATUS', 'Auth', 'Status'),
       ('AUTH_NEW_USER', 'Auth', 'New-User'),
       ('AUTH_RO_USER', 'Ryan-Auth', 'Orton'),
       ('AUTH_RO_VARY_USER', 'Ryan-Auth-Vary', 'Orton'),
       ('AUTH_RO_USER_TEST', 'Ryan-Auth', 'Orton'),
       ('AUTH_GROUP_MANAGER', 'Group', 'Manager');

INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_ADM' from roles where role_code = 'OAUTH_ADMIN';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_ADM' from roles where role_code = 'MAINTAIN_ACCESS_ROLES';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_ADM' from roles where role_code = 'MAINTAIN_OAUTH_USERS';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_RO_USER' from roles where role_code = 'LICENCE_RO';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_RO_USER' from roles where role_code = 'GLOBAL_SEARCH';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_RO_VARY_USER' from roles where role_code = 'LICENCE_RO';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_RO_VARY_USER' from roles where role_code = 'GLOBAL_SEARCH';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_RO_VARY_USER' from roles where role_code = 'LICENCE_VARY';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_RO_USER_TEST' from roles where role_code = 'LICENCE_RO';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_RO_USER_TEST' from roles where role_code = 'GLOBAL_SEARCH';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_GROUP_MANAGER' from roles where role_code = 'AUTH_GROUP_MANAGER';
INSERT INTO user_email_roles (role_id, username) SELECT role_id, 'AUTH_DELETEALL' from roles where role_code = 'LICENCE_RO';

INSERT INTO groups (group_id, group_code, group_name)
VALUES (newid(), 'SITE_1_GROUP_1', 'Site 1 - Group 1'),
       (newid(), 'SITE_1_GROUP_2', 'Site 1 - Group 2'),
       (newid(), 'SITE_2_GROUP_1', 'Site 2 - Group 1'),
       (newid(), 'SITE_3_GROUP_1', 'Site 3 - Group 1');

INSERT INTO user_email_groups (groups_group_id, useremail_username) SELECT group_id, 'AUTH_RO_VARY_USER' FROM groups WHERE group_code = 'SITE_1_GROUP_1';
INSERT INTO user_email_groups (groups_group_id, useremail_username) SELECT group_id, 'AUTH_RO_VARY_USER' FROM groups WHERE group_code = 'SITE_1_GROUP_2';
INSERT INTO user_email_groups (groups_group_id, useremail_username) SELECT group_id, 'AUTH_RO_USER' FROM groups WHERE group_code = 'SITE_1_GROUP_1';
INSERT INTO user_email_groups (groups_group_id, useremail_username) SELECT group_id, 'AUTH_RO_USER_TEST' FROM groups WHERE group_code = 'SITE_1_GROUP_1';
INSERT INTO user_email_groups (groups_group_id, useremail_username) SELECT group_id, 'AUTH_RO_USER_TEST' FROM groups WHERE group_code = 'SITE_2_GROUP_1';
INSERT INTO user_email_groups (groups_group_id, useremail_username) SELECT group_id, 'AUTH_GROUP_MANAGER' FROM groups WHERE group_code = 'SITE_1_GROUP_1';
INSERT INTO user_email_groups (groups_group_id, useremail_username) SELECT group_id, 'AUTH_GROUP_MANAGER' FROM groups WHERE group_code = 'SITE_1_GROUP_2';
INSERT INTO user_email_groups (groups_group_id, useremail_username) SELECT group_id, 'AUTH_DELETEALL' FROM groups WHERE group_code = 'SITE_3_GROUP_1';

INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'true' FROM groups g, roles r WHERE r.role_code = 'GLOBAL_SEARCH' AND g.group_code = 'SITE_1_GROUP_1';
INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'true' FROM groups g, roles r WHERE r.role_code = 'LICENCE_RO' AND g.group_code = 'SITE_1_GROUP_1';
INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'false' FROM groups g, roles r WHERE r.role_code = 'LICENCE_VARY' AND g.group_code = 'SITE_1_GROUP_1';
INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'true' FROM groups g, roles r WHERE r.role_code = 'GLOBAL_SEARCH' AND g.group_code = 'SITE_1_GROUP_2';
INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'true' FROM groups g, roles r WHERE r.role_code = 'LICENCE_RO' AND g.group_code = 'SITE_1_GROUP_2';
