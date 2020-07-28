INSERT INTO roles (role_id, role_code, role_name) values (NEWID(), 'PECS_COURT', 'PECS Court User');
INSERT INTO roles (role_id, role_code, role_name) values (NEWID(), 'PECS_PER_AUTHOR', 'PECS Person Escort Record Author');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_GLDFMC', 'PECS Court Guildford MC'),
       (NEWID(), 'PECS_DORKMC', 'PECS Court Dorking MC'),
       (NEWID(), 'PECS_RDHLMC', 'PECS Court Redhill MC'),
       (NEWID(), 'PECS_RCHTMC', 'PECS Court Richmond upon Thames MC'),
       (NEWID(), 'PECS_SUTTMC', 'PECS Court Sutton MC'),
       (NEWID(), 'PECS_LEEDCC', 'PECS Court Leeds Crown Court');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_GLDFMC'),
        1);
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_DORKMC'),
        1);
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_RDHLMC'),
        1);
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_RCHTMC'),
        1);
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_SUTTMC'),
        1);
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_LEEDCC'),
        1);
