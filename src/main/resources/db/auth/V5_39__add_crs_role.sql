INSERT INTO roles (role_id, role_code, role_name)
VALUES (newid(), 'CRS_PROVIDER', 'Commissioned rehabilitative services (CRS) provider');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
  SELECT role_id, g.group_id, 1
  FROM roles r, groups g
  WHERE r.role_code = 'CRS_PROVIDER' AND g.group_code LIKE 'INT_SP_%';
