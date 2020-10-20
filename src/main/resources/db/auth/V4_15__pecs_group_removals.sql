delete
from user_group
where group_id in
      (select group_id
       from groups
       where group_code in
             ('PECS_WARWBC', 'PECS_MPS63', 'PECS_NEWCBC')
      );

delete
from group_assignable_role
where group_id in
      (select group_id
       from groups
       where group_code in
             ('PECS_WARWBC', 'PECS_MPS63', 'PECS_NEWCBC')
      );

delete
from groups
where group_code in
      ('PECS_WARWBC', 'PECS_MPS63', 'PECS_NEWCBC');
