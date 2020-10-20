INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_SLOUMC', 'PECS Court Slough Magistrates Court');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_SLOUMC'),
        1);

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_SOUTMC', 'PECS Court Southampton (WH) Magistrates Court');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_SOUTMC'),
        1);

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_WNCHCT', 'PECS Court Winchester County Court');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_WNCHCT'),
        1);

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_GMP13', 'PECS Manchester Airport Police Station');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_POLICE'),
        (select group_id from groups where group_code in ('PECS_GMP13')),
        1);

INSERT INTO user_group (group_id, user_id)
SELECT group_id, user_id
from groups,
     users
where group_code = 'PECS_GMP13'
  and user_id in
      (select user_id
       from user_group
                join groups r2 on r2.group_id = user_group.group_id
       where r2.group_code = 'PECS_GMP8')



