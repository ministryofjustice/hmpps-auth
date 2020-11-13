delete from child_group
    where child_group_code ='PECS_DST4';
delete from child_group
    where child_group_code ='PECS_DST6';
delete from child_group
    where child_group_code ='PECS_MPS28';

update child_group
    set child_group_name='PECS Keynsham Police Centre'
        where child_group_code='PECS_AVS1';
update child_group
    set child_group_name='PECS Wakefield District Police Station'
        where child_group_code='PECS_WYP19';
update child_group
    set child_group_name='PECS St. Mary''s Wharf Police Station'
        where child_group_code='PECS_DRB3';

INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    (select newid(), 'PECS_DRB9', 'PECS Ilkeston Police Station', group_id from groups where group_code = 'PECS_DRB');
INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    (select newid(), 'PECS_ESX8', 'PECS Braintree Police Station', group_id from groups where group_code = 'PECS_ESX');
INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    (select newid(), 'PECS_NFL6',' PECS Norwich City Police Station', group_id from groups where group_code = 'PECS_NFL');
