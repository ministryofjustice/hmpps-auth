INSERT INTO roles (role_id, role_code, role_name)
VALUES (newid(), 'SOC_HQ', 'SOC Headquarters User');

INSERT INTO groups (group_id, group_code, group_name)
    VALUES (NEWID(), 'SOC_HQ', 'SOC Headquarters');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
    VALUES ((select role_id from roles where role_code = 'SOC_HQ'),
        (select group_id from groups where group_code = 'SOC_HQ'),
        1);

