delete from user_role where role_id in
(select role_id from roles where role_code in ('MAINTAIN_ACCESS_ROLES', 'MAINTAIN_ACCESS_ROLES_ADMIN'));

delete from roles where role_code in ('MAINTAIN_ACCESS_ROLES', 'MAINTAIN_ACCESS_ROLES_ADMIN');
