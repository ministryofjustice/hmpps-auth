INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    (select newid(), 'PECS_NWA8', 'PECS Police Llandudno Police Station', group_id from groups where group_code = 'PECS_NWA');

INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    (select newid(), 'PECS_NWA9', 'PECS Police Llangefni Police Station', group_id from groups where group_code = 'PECS_NWA');

INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    (select newid(), 'PECS_NWA10', 'PECS Police Prestatyn Police Station', group_id from groups where group_code = 'PECS_NWA');

INSERT INTO child_group (child_group_id, child_group_code, child_group_name, group_id)
    (select newid(), 'PECS_NWA11', 'PECS Police Rhyl Police Station', group_id from groups where group_code = 'PECS_NWA');
