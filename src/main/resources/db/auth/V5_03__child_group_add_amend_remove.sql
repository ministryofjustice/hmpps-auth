delete from child_group
    where child_group_code ='DST4';
delete from child_group
    where child_group_code ='DST6';
delete from child_group
    where child_group_code ='MPS28';

update child_group
    set child_group_name='PECS Keynsham Police Centre'
        where child_group_code='AVS1';
update child_group
    set child_group_name='PECS Wakefield District Police Station'
        where child_group_code='WYP19';

INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    VALUES (newid(), 'DRB8', 'PECS Ilkeston Police Station', (select group_id from groups where group_code = 'DRB'));

INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    VALUES (newid(), 'DRB9', 'PECS St. Marys Wharf Police Station', (select group_id from groups where group_code = 'DRB'));

INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    VALUES (newid(), 'DP8', 'PECS Braintree Police Station', (select group_id from groups where group_code = 'DP'));

INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    VALUES (newid(), 'NFL6',' PECS Norwich City Police Station', (select group_id from groups where group_code = 'NFL'));
