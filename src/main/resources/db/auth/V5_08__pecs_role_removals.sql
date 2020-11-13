delete
from user_role
where role_id in
      (select role_id
       from roles
       where role_code = 'PECS_HMYOI');

delete
from group_assignable_role
where role_id in
      (select role_id
       from roles
       where role_code = 'PECS_HMYOI');

delete
from roles
where role_code = 'PECS_HMYOI';
