INSERT INTO roles (role_id, role_code, role_name) values (NEWID(), 'SOC_CUSTODY', 'SOCU Prison Role');
INSERT INTO roles (role_id, role_code, role_name) values (NEWID(), 'SOC_COMMUNITY', 'SOCU Probation Role');
INSERT INTO roles (role_id, role_code, role_name) values (NEWID(), 'SOC_RESTRICTED', 'SOCU Restricted Caseload');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'SOC_NORTH_EAST', 'SOCU North East');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'SOC_NORTH_WEST', 'SOCU North West');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'SOC_RESTRICTED'),
        (select group_id from groups where group_code = 'SOC_NORTH_EAST'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'SOC_RESTRICTED'),
        (select group_id from groups where group_code = 'SOC_NORTH_WEST'),
        1);
