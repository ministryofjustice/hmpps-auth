INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_WARWBC', 'PECS Court Warwickshire Justice Centre (L''ton Spa)');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code in ('PECS_WARWBC')), 1);

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_BCCACC', 'PECS Court Birmingham Crown Court (Annex)');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code in ('PECS_BCCACC')), 1);

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_NWCSMC', 'PECS Court Newcastle-Upon-Tyne Magistrates Court');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code in ('PECS_NWCSMC')), 1);

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_PRTTMC', 'PECS Court Port Talbot Magistrates Court');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code in ('PECS_PRTTMC')), 1);

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_SUS7', 'PECS Chichester Police Station');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_POLICE'),
        (select group_id from groups where group_code in ('PECS_SUS7')), 1);

INSERT INTO user_group (group_id, user_id)
SELECT group_id, user_id
from groups,
     users
where group_code = 'PECS_SUS7'
  and user_id in (select user_id
                  from user_group
                           join groups r2 on r2.group_id = user_group.group_id
                  where r2.group_code = 'PECS_SUS5');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_MPS34', 'PECS Harrow Road Police Station');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_POLICE'),
        (select group_id from groups where group_code in ('PECS_MPS34')), 1);
INSERT INTO user_group (group_id, user_id)
SELECT group_id, user_id
from groups,
     users
where group_code = 'PECS_MPS34'
  and user_id in (select user_id
                  from user_group
                           join groups r2 on r2.group_id = user_group.group_id
                  where r2.group_code = 'PECS_MPS33');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_WLT2', 'PECS Chippenham Police Station');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_POLICE'),
        (select group_id from groups where group_code in ('PECS_WLT2')), 1);
INSERT INTO user_group (group_id, user_id)
SELECT group_id, user_id
from groups,
     users
where group_code = 'PECS_WLT2'
  and user_id in (select user_id
                  from user_group
                           join groups r2 on r2.group_id = user_group.group_id
                  where r2.group_code = 'PECS_WLT1');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_DRB8', 'PECS Butterley Police Station');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_POLICE'),
        (select group_id from groups where group_code in ('PECS_DRB8')), 1);
INSERT INTO user_group (group_id, user_id)
SELECT group_id, user_id
from groups,
     users
where group_code = 'PECS_DRB8'
  and user_id in (select user_id
                  from user_group
                           join groups r2 on r2.group_id = user_group.group_id
                  where r2.group_code = 'PECS_DRB7');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_NWA6', 'PECS Holyhead (Anglesey) Police Station');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_POLICE'),
        (select group_id from groups where group_code in ('PECS_NWA6')), 1);
INSERT INTO user_group (group_id, user_id)
SELECT group_id, user_id
from groups,
     users
where group_code = 'PECS_NWA6'
  and user_id in (select user_id
                  from user_group
                           join groups r2 on r2.group_id = user_group.group_id
                  where r2.group_code = 'PECS_NWA5');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'PECS_MRS9', 'PECS St Helens Police Station');
INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'PECS_POLICE'),
        (select group_id from groups where group_code in ('PECS_MRS9')), 1);
INSERT INTO user_group (group_id, user_id)
SELECT group_id, user_id
from groups,
     users
where group_code = 'PECS_MRS9'
  and user_id in (select user_id
                  from user_group
                           join groups r2 on r2.group_id = user_group.group_id
                  where r2.group_code = 'PECS_MRS4');

UPDATE groups
set group_name = 'PECS St Asaph Police Station'
where group_code = 'PECS_NWA4';
UPDATE groups
set group_name = 'PECS Court Central Family Court'
where group_code = 'PECS_PRINCT';
