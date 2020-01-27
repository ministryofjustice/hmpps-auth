delete from user_role where role_id =
(select role_id from roles where role_code = 'PATHFINDER_OM');

delete from roles
where role_code = 'PATHFINDER_OM';
