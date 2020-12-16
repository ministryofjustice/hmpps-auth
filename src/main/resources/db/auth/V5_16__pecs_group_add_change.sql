insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_WARNCC', 'PECS Court Warrington Crown Court');
insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_WARRMC', 'PECS Court Warrington Magistrates Court');
insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_NEUTCC', 'PECS Court Newcastle upon Tyne Crown Court (Quayside)');
insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_NUTMQC', 'PECS Court Newcastle upon Tyne Magistrates Court (Quayside)');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code ='PECS_WARNCC'),
        1);
insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_WARRMC'),
        1);
insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_NEUTCC'),
        1);
insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_NUTMQC'),
        1);

update groups set group_name = 'PECS Court Newcastle upon Tyne Magistrates Court'
    where group_code = 'PECS_NWCSMC';
update groups set group_name = 'PECS Court Newcastle upon Tyne County Court'
    where group_code = 'PECS_NWCSCT';
update groups set group_name = 'PECS Court Newcastle upon Tyne Crown Court (Moot Hall)'
    where group_code = 'PECS_NWCSCC';
