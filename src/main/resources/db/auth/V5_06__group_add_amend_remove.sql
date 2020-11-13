update groups  set group_name='PECS Court Barrow In Furness Combined Court'
    where group_code='PECS_BARRBC';

update groups  set group_code='PECS_NRTSMC', group_name='PECS Court North Shields Magistrates Court'
    where group_code='PECS_NORTMC';


INSERT INTO groups (group_id, group_code, group_name)
    VALUES (NEWID(), 'PECS_AMERCC', 'PECS Court Amersham Crown Court');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
    VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_AMERCC'),
        1);

INSERT INTO groups (group_id, group_code, group_name)
    VALUES (NEWID(), 'PECS_CUCOCT', 'PECS Court Surrey Coroners Court');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
    VALUES ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_CUCOCT'),
        1);