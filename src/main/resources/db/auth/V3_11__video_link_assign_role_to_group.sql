INSERT INTO group_assignable_role (role_id, group_id, automatic)
SELECT role_id, group_id, 'true'
FROM groups g, roles r
WHERE r.role_code = 'VIDEO_LINK_COURT_USER'
AND g.group_code = 'VIDEO_LINK_COURT_USER';
