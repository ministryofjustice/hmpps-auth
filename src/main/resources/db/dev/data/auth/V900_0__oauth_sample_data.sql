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
       ('prisonstaffhubclient','3600',null,'ROLE_SYSTEM_READ_ONLY,ROLE_COMMUNITY','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',43200,null,'read',null),
       ('risk-profiler','3600',null,'ROLE_SYSTEM_USER,ROLE_RISK_PROFILER','client_credentials',null,'$2a$10$r12DB/sqXduodnjtAY/ykO0S3KCySdVW4zhG3jlIRaIsfVkFOEds2',null,null,'read',null),
       ('community-api-client','3600',null,'ROLE_SYSTEM_USER,ROLE_COMMUNITY','client_credentials','proxy-user','$2a$10$r12DB/sqXduodnjtAY/ykO0S3KCySdVW4zhG3jlIRaIsfVkFOEds2',null,null,'read,proxy-user',null),
       ('sentence-plan-client','3600',null,null,'authorization_code,refresh_token','read,write','$2a$10$a5WJN/AZc7Nq3rFoy5GOQ.avY.opPq/RaF59TXFaInt0Jxp6NV94a',43200,null,'read,write','http://localhost:3000/login/callback,http://localhost:3000'),
       ('use-of-force-client','3600',null,null,'authorization_code,refresh_token','read,write','$2a$10$YRkR9FGSpZu3FAn5.Awtk.Yd0hg92y63VfVVAKhS6k66nMsc3/Hiy',43200,null,'read,write','http://localhost:3000/login/callback,http://localhost:3000'),
       ('whereabouts-api-client','3600',null,'ROLE_PAY, ROLE_CASE_NOTE_ADMIN','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',null,null,'read,write',null),
       ('pathfinder-client','3600',null,null,'authorization_code,refresh_token','read,write','$2a$10$WzgtydqXSuhdivpWDR3WXO.yjLBm4yuDqP64Og.7E4XURdrSfhOTi',43200,null,'read,write','http://localhost:3000/login/callback,http://localhost:3000'),
       ('pathfinder-admin','3600',null,'ROLE_SYSTEM_USER','client_credentials','read,write','$2a$10$ajGimbJNWF1/FmZQMJWvieeQ/OdYaxWHQPgOjYDvvWu/4/744Yw7S',43200,null,'read,write',null),
       ('offender-events-client','1200',null,'ROLE_SYSTEM_READ_ONLY,ROLE_SYSTEM_USER','client_credentials','read','$2a$10$.95l4ENV1OEZ6qWd4R5QTOXZrjvTQmN402z1pjRUr2EwGFYdkDDnm',null,null,'read',null),
       ('sentence-plan-api-client','3600',null,'ROLE_OASYS_READ_ONLY','client_credentials',null,'$2a$10$lBwbziQlLfiCnn8Kj1PfMujEcLdsJYlYSNJvBRO638gCYTS9yN0xm',null,null,'read',null);


INSERT INTO oauth_service (code, name, description, authorised_roles, url, enabled, email)
VALUES ('BOOK_MOVE', 'Book a secure move', 'Book a secure move', 'ROLE_PECS_SUPPLIER,ROLE_PECS_POLICE,', 'https://bookasecuremove.service.justice.gov.uk', 1, 'bookasecuremove@digital.justice.gov.uk'),
       ('CATTOOL', 'Digital Categorisation Service', 'Service to support categorisation of prisoners providing a consistent workflow and risk indicators.', 'ROLE_CREATE_CATEGORISATION,ROLE_APPROVE_CATEGORISATION,ROLE_CATEGORISATION_SECURITY,ROLE_CREATE_RECATEGORISATION', 'https://offender-categorisation.service.justice.gov.uk', 1, 'categorisation@justice.gov.uk'),
       ('HDC', 'Home Detention Curfew', 'Service for HDC Licences Creation and Approval', 'ROLE_LICENCE_CA,ROLE_LICENCE_RO,ROLE_LICENCE_DM', 'http://localhost:3003', 1, 'hdcdigitalservice@digital.justice.gov.uk'),
       ('KW', 'Keyworker Management Service', 'Service to allow viewing and allocation of Key workers to prisoners and viewing of prison and staff level statistics.', 'ROLE_OMIC_ADMIN,ROLE_KEYWORKER_MONITOR', 'http://localhost:3001/manage-key-workers', 1, null),
       ('NOMIS', 'Digital Prison Service', 'View and Manage Offenders in Prison (Old name was NEW NOMIS)', null, 'http://localhost:3000', 1, 'feedback@digital.justice.gov.uk'),
       ('OAUTHADMIN', 'Oauth Client Management', 'Manage Client Credentials for OAUTH2 Clients', 'ROLE_OAUTH_ADMIN', 'http://localhost:8080/auth/ui/', 1, null),
       ('POM', 'Allocate a POM Service', 'Allocate the appropriate offender manager to a prisoner', 'ROLE_ALLOC_MGR', 'https://moic.service.justice.gov.uk', 1, 'https://moic.service.justice.gov.uk/help'),
       ('PATHFINDER', 'Pathfinder Service', 'View and Manage Pathfinder nominals', 'ROLE_PATHFINDER_OM,ROLE_PATHFINDER_PPL,ROLE_PATHFINDER_IC', 'http://localhost:3000', 1, null),
       ('USERADMIN', 'Admin & Utilities Service', 'Admin & utilities Service For NOMIS and Auth User', 'ROLE_KW_MIGRATION,ROLE_MAINTAIN_ACCESS_ROLES,ROLE_MAINTAIN_ACCESS_ROLES_ADMIN,ROLE_MAINTAIN_OAUTH_USERS,ROLE_AUTH_GROUP_MANAGER', 'http://localhost:3001/admin-utilities', 1, null);


INSERT INTO user_retries (username, retry_count)
VALUES ('LOCKED_USER', 5),
       ('AUTH_DELETEALL', 3),
       ('NOMIS_DELETE', 1);


-- nomis users
INSERT INTO users (user_id, username, email, verified)
 VALUES ('07395EF9-53EC-4D6C-8BB1-0DC96CD4BD2F', 'IC_USER', 'ic_user@digital.justice.gov.uk', 1),
        ('A04C70EE-51C9-4852-8D0D-130DA5C85C42', 'ITAG_USER', 'itag_user@digital.justice.gov.uk', 1),
        ('0181F647-C7D4-41E7-9271-288EC7C01F90', 'DM_USER', 'dm_user@digital.justice.gov.uk', 0),
        ('151DD6BC-88EE-4246-AA18-45924819C9F5', 'EXPIRED_TEST_USER', 'expired_test_user@digital.justice.gov.uk', 1),
        ('86192295-8652-40BB-B03F-4D56BB93C1D7', 'RESET_TEST_USER', 'reset_test@digital.justice.gov.uk', 1),
        ('F566EEC3-32DD-4CA4-B477-56AEC62917A1', 'CA_USER_TEST', 'reset_test@digital.justice.gov.uk', 1),
        ('846D7318-921C-4537-8E24-58306CED881B', 'PPL_USER', 'ppl_user@digital.justice.gov.uk', 1),
        ('326A07B4-6C2F-4CF9-A904-84262EB5C4FF', 'DM_USER_TEST', 'dm_user_test@digital.justice.gov.uk', 1),
        ('7969D655-03F6-464E-A318-9D3C8B53787A', 'EXPIRED_TEST2_USER', 'expired_test2_user@digital.justice.gov.uk', 1),
        ('FCFC15C1-6EE5-4EB2-8312-A1302AE3CFD1', 'ITAG_USER_ADM', 'itag_user_adm@digital.justice.gov.uk', 1),
        ('E94B2E26-DC8A-4020-9533-A509807F68DF', 'EXPIRED_TEST3_USER', 'expired_test3_user@digital.justice.gov.uk', 1),
        ('C0279EE3-76BF-487F-833C-AA47C5DF22F8', 'CA_USER', 'ca_user@digital.justice.gov.uk', 1),
        ('6A3F0216-BBAB-49CD-BD6E-AC09C1762EE4', 'LOCKED_USER', 'locked@somewhere.com', 1),
        ('79CDC23C-510F-4CE2-8C98-AC251296EC39', 'RO_DEMO', null, 0),
        ('AB8DA2CA-3E79-42D3-883E-CEE6C3F693CA', 'RO_USER_TEST', 'ro_user_test@digital.justice.gov.uk', 1),
        ('5C72B180-5211-454D-9605-CF29573B946F', 'UOF_REVIEWER_USER', 'uof_reviewer@digital.justice.gov.uk', 1),
        ('98FBF8D7-4164-47B3-826F-ECD3BB643005', 'RCTL_USER1', 'rctl_user1@digital.justice.gov.uk', 1),
        ('26912E22-E313-4164-8434-0D2623483123', 'PPL_RCTL_USER1', 'ppl_rctl_user1@digital.justice.gov.uk', 1);

INSERT INTO users (user_id, username, email, verified, last_logged_in)
 VALUES ('A2B6E3C0-2CE4-4148-9DFB-42E94BC78D02', 'NOMIS_DELETE', 'locked@somewhere.com', 1, '2018-02-04 13:23:19.0000000');

-- auth users
INSERT INTO users (user_id, username, password, password_expiry, email, first_name, last_name, verified, enabled, locked, master)
 VALUES ('608955AE-52ED-44CC-884C-011597A77949', 'AUTH_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_user@digital.justice.gov.uk', 'Auth', 'Only', 1, 1, 0, 1),
        ('C36E5A30-53C7-4F6F-9591-0547A2E4897C', 'AUTH_NO_EMAIL', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', null, 'Auth', 'NoEmail', 1, 1, 0, 1),
        ('0E7AFB2E-A326-4AB6-920C-0A7098F5031F', 'AUTH_LOCKED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', null, 'Auth', 'Locked', 1, 1, 1, 1),
        ('90F930E1-2195-4AFD-92CE-0EB5672DA02A', 'AUTH_RO_USER_TEST', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_ro_user_test@digital.justice.gov.uk', 'Ryan-Auth', 'Orton', 1, 1, 0, 1),
        ('D9873CB3-24BD-4CFF-9CFE-1E64CE6BBCC4', 'AUTH_LOCKED2', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_locked2@digital.justice.gov.uk', 'Auth', 'Locked2', 1, 1, 1, 1),
        ('5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8', 'AUTH_RO_VARY_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_ro_user@digital.justice.gov.uk', 'Ryan-Auth-Vary', 'Orton', 1, 1, 0, 1),
        ('AD7D37E2-DBAD-4B98-AF8D-429E822A6BDC', 'AUTH_DISABLED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', null, 'Auth', 'Disabled', 1, 0, 0, 1),
        ('7CA04ED7-8275-45B2-AFB4-4FF51432D1EA', 'AUTH_RO_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_ro_user@digital.justice.gov.uk', 'Ryan-Auth', 'Orton', 1, 1, 0, 1),
        ('1F650F15-0993-4DB7-9A32-5B930FF86035', 'AUTH_GROUP_MANAGER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_group_manager@digital.justice.gov.uk', 'Group', 'Manager', 1, 1, 0, 1),
        ('7CC5368B-C912-47AC-AB7E-7ED0DC832AE4', 'AUTH_PF_OM_USER', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_pf_om_user@digital.justice.gov.uk', 'Offender', 'Manager', 1, 1, 0, 1),
        ('FC494152-F9AD-48A0-A87C-9ADC8BD75255', 'AUTH_STATUS', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', null, 'Auth', 'Status', 1, 0, 0, 1),
        ('9E84F1E4-59C8-4B10-927A-9CF9E9A30791', 'AUTH_EXPIRED', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2013-01-28 13:23:19.0000000', 'auth_test2@digital.justice.gov.uk', 'Auth', 'Expired', 1, 1, 0, 1),
        ('5105A589-75B3-4CA0-9433-B96228C1C8F3', 'AUTH_ADM', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_test2@digital.justice.gov.uk', 'Auth', 'Adm', 1, 1, 0, 1),
        ('2E285CCD-DCFD-4497-9E22-D6E8E10A2A3E', 'AUTH_TEST', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '3013-01-28 13:23:19.0000000', 'auth_test@digital.justice.gov.uk', 'Auth', 'Test', 1, 1, 0, 1),
        ('67A789DE-7D29-4863-B9C2-F2CE715DC4BC', 'AUTH_NEW_USER', null, '3013-01-28 13:23:19.0000000', 'a@b.com', 'Auth', 'New-User', 0, 1, 0, 1);

INSERT INTO users (user_id, username, password, last_logged_in, first_name, last_name, verified, enabled, locked, master)
 VALUES ('7B59A818-BC14-43F3-A1C3-93004E173B2A', 'AUTH_DELETE', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2018-01-02 13:23:19.0000000', 'Auth', 'Delete', 1, 0, 0, 1),
        ('DA28D339-85FA-42C1-9CFA-AC67055A51A5', 'AUTH_INACTIVE', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2019-02-03 13:23:19.0000000', 'Auth', 'Inactive', 1, 1, 0, 1),
        ('7112EC3B-88C1-48C3-BCC3-F82874E3F2C3', 'AUTH_DELETEALL', '{bcrypt}$2a$10$Fmcp2KUKRW53US3EJfsxkOh.ekZhqz5.Baheb9E98QLwEFLb9csxy', '2018-02-03 13:23:19.0000000', 'Auth', 'DeleteAll', 1, 0, 0, 1);

INSERT INTO user_token (token, token_type, token_expiry, user_id)
 VALUES ('reset', 'RESET', '2018-12-10 08:55:45.0000000', '6A3F0216-BBAB-49CD-BD6E-AC09C1762EE4'),
        ('reset2', 'RESET', '2018-12-10 08:55:45.0000000', '7112EC3B-88C1-48C3-BCC3-F82874E3F2C3'),
        ('verified', 'VERIFIED', '2018-12-10 08:55:45.0000000', '7112EC3B-88C1-48C3-BCC3-F82874E3F2C3');

INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_ADM' and role_code = 'OAUTH_ADMIN';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_ADM' and role_code = 'MAINTAIN_ACCESS_ROLES';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_ADM' and role_code = 'MAINTAIN_OAUTH_USERS';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_RO_USER' and role_code = 'LICENCE_RO';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_RO_USER' and role_code = 'GLOBAL_SEARCH';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_RO_VARY_USER' and role_code = 'LICENCE_RO';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_RO_VARY_USER' and role_code = 'GLOBAL_SEARCH';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_RO_VARY_USER' and role_code = 'LICENCE_VARY';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_RO_USER_TEST' and role_code = 'LICENCE_RO';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_RO_USER_TEST' and role_code = 'GLOBAL_SEARCH';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_GROUP_MANAGER' and role_code = 'AUTH_GROUP_MANAGER';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_DELETEALL' and role_code = 'LICENCE_RO';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_DELETEALL' and role_code = 'LICENCE_RO';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_PF_OM_USER' and role_code = 'PATHFINDER_OM';
INSERT INTO user_role (role_id, user_id) SELECT role_id, user_id from roles, users where username = 'AUTH_PF_OM_USER' and role_code = 'GLOBAL_SEARCH';

INSERT INTO groups (group_id, group_code, group_name) VALUES (newid(), 'SITE_1_GROUP_1', 'Site 1 - Group 1'),
       (newid(), 'SITE_1_GROUP_2', 'Site 1 - Group 2'),
       (newid(), 'SITE_2_GROUP_1', 'Site 2 - Group 1'),
       (newid(), 'SITE_3_GROUP_1', 'Site 3 - Group 1');

INSERT INTO user_group (group_id, user_id) SELECT group_id, user_id from groups, users where username = 'AUTH_RO_VARY_USER' and group_code = 'SITE_1_GROUP_1';
INSERT INTO user_group (group_id, user_id) SELECT group_id, user_id from groups, users where username = 'AUTH_RO_VARY_USER' and group_code = 'SITE_1_GROUP_2';
INSERT INTO user_group (group_id, user_id) SELECT group_id, user_id from groups, users where username = 'AUTH_RO_USER' and group_code = 'SITE_1_GROUP_1';
INSERT INTO user_group (group_id, user_id) SELECT group_id, user_id from groups, users where username = 'AUTH_RO_USER_TEST' and group_code = 'SITE_1_GROUP_1';
INSERT INTO user_group (group_id, user_id) SELECT group_id, user_id from groups, users where username = 'AUTH_RO_USER_TEST' and group_code = 'SITE_2_GROUP_1';
INSERT INTO user_group (group_id, user_id) SELECT group_id, user_id from groups, users where username = 'AUTH_GROUP_MANAGER' and group_code = 'SITE_1_GROUP_1';
INSERT INTO user_group (group_id, user_id) SELECT group_id, user_id from groups, users where username = 'AUTH_GROUP_MANAGER' and group_code = 'SITE_1_GROUP_2';
INSERT INTO user_group (group_id, user_id) SELECT group_id, user_id from groups, users where username = 'AUTH_DELETEALL' and group_code = 'SITE_3_GROUP_1';

INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'true' FROM groups g, roles r WHERE r.role_code = 'GLOBAL_SEARCH' AND g.group_code = 'SITE_1_GROUP_1';
INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'true' FROM groups g, roles r WHERE r.role_code = 'LICENCE_RO' AND g.group_code = 'SITE_1_GROUP_1';
INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'false' FROM groups g, roles r WHERE r.role_code = 'LICENCE_VARY' AND g.group_code = 'SITE_1_GROUP_1';
INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'true' FROM groups g, roles r WHERE r.role_code = 'GLOBAL_SEARCH' AND g.group_code = 'SITE_1_GROUP_2';
INSERT INTO group_assignable_role (role_id, group_id, automatic) SELECT role_id, group_id, 'true' FROM groups g, roles r WHERE r.role_code = 'LICENCE_RO' AND g.group_code = 'SITE_1_GROUP_2';
