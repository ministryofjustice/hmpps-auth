INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_NWPRMC', 'PECS Court Newport (I.O.W.) MC');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_NWPRMC'),
        1);
