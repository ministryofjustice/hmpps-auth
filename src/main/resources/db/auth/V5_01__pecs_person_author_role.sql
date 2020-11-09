INSERT INTO group_assignable_role (role_id, group_id, automatic)
select role_id, group_id, 1 from groups, roles
where group_code like 'PECS_%'
  and group_name not like 'PECS Court%'
  and role_code = 'PECS_PER_AUTHOR'
