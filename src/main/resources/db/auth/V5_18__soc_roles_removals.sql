delete
from user_role
where role_id in
      (select role_id
       from roles
       where role_code = 'SOC_RESTRICTED');

delete
from group_assignable_role
where role_id in
      (select role_id
       from roles
       where role_code = 'SOC_RESTRICTED');

delete
from roles
where role_code = 'SOC_RESTRICTED';
