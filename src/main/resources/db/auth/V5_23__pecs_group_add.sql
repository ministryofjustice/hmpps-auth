INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_STOKCC', 'PECS Court Stoke-on-Trent Crown Court'),
       (NEWID(), 'PECS_HUNTCC', 'PECS Court Huntington Crown Court');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_STOKCC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_HUNTCC'),
        1);
