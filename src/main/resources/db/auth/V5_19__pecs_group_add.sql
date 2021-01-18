INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_BOLTCC', 'PECS Court Bolton Crown Court'),
       (NEWID(), 'PECS_BOLTMC', 'PECS Court Bolton Magistrates Court'),
       (NEWID(), 'PECS_EXETMC', 'PECS Court Exeter Magistrates Court'),
       (NEWID(), 'PECS_EXETCC', 'PECS Court Exeter Crown Court'),
       (NEWID(), 'PECS_GTSHMC', 'PECS Court Gateshead Magistrates Court'),
       (NEWID(), 'PECS_HVRFMC', 'PECS Court Haverfordwest Magistrates Court'),
       (NEWID(), 'PECS_HUNTMC', 'PECS Court Huntingdon Magistrates Court'),
       (NEWID(), 'PECS_IOWICC', 'PECS Court Newport (IoW) Crown Court'),
       (NEWID(), 'PECS_MRTHCC', 'PECS Court Merthyr Tydfil Crown Court'),
       (NEWID(), 'PECS_MRTHMC', 'PECS Court Merthyr Tydfil Magistrates Court'),
       (NEWID(), 'PECS_MOLDCC', 'PECS Court Mold Crown Court'),
       (NEWID(), 'PECS_MOLDMC', 'PECS Court Mold Magistrates Court'),
       (NEWID(), 'PECS_SALSCC', 'PECS Court Salisbury Crown Court'),
       (NEWID(), 'PECS_SALSMC', 'PECS Court Salisbury Magistrates Court'),
       (NEWID(), 'PECS_SUNDMC', 'PECS Court Sunderland Magistrates Court');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_BOLTCC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_BOLTMC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_EXETMC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_EXETCC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_GTSHMC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_HVRFMC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_HUNTMC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_IOWICC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_MRTHCC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_MRTHMC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_MOLDCC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_MOLDMC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_SALSCC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_SALSMC'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_SUNDMC'),
        1);
