delete
from user_group
where group_id in
      (select group_id
       from groups
       where group_code = 'PECS_STTCYC');

delete
from group_assignable_role
where group_id in
      (select group_id
       from groups
       where group_code = 'PECS_STTCYC');

delete
from groups
where group_code ='PECS_STTCYC';
