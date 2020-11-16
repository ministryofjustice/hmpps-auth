delete
from user_group
where group_id in
      (select group_id
       from groups
       where group_code in
             ('PECS_CKI', 'PECS_FMI', 'PECS_PRI', 'PECS_WNI', 'PECS_WYI')
      );

delete
from group_assignable_role
where group_id in
      (select group_id
       from groups
       where group_code in
             ('PECS_CKI', 'PECS_FMI', 'PECS_PRI', 'PECS_WNI', 'PECS_WYI')
      );

delete
from groups
where group_code in
      ('PECS_CKI', 'PECS_FMI', 'PECS_PRI', 'PECS_WNI', 'PECS_WYI');
