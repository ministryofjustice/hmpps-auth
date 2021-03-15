INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_SNAACC', 'PECS Court Snaresbrook Crown Court (Annex)');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_SNAACC'),
        1);
