insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_PRCORC', 'PECS Court Preston Coroners Court');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_PRCORC'),
        1);
