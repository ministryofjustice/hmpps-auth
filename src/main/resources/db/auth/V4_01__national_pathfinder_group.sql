-- only insert role if doesn't exist already
INSERT INTO roles (role_id, role_code, role_name)
SELECT NEWID(), 'PF_POLICE', 'Pathfinder Police'
FROM roles
WHERE role_code = 'GLOBAL_SEARCH'
  AND NOT exists(SELECT role_code FROM roles WHERE role_code = 'PF_POLICE');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PF_NATIONAL', 'Pathfinder Police - National');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PF_POLICE'),
        (select group_id from groups where group_code = 'PF_NATIONAL'),
        1);
