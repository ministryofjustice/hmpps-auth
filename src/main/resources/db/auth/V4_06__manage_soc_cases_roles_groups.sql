INSERT INTO roles (role_id, role_code, role_name) values (NEWID(), 'SOC_CUSTODY', 'SOC Prison Role');
INSERT INTO roles (role_id, role_code, role_name) values (NEWID(), 'SOC_COMMUNITY', 'SOC Probation Role');
INSERT INTO roles (role_id, role_code, role_name) values (NEWID(), 'SOC_RESTRICTED', 'SOC Restricted Caseload');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'SOC_NORTH_EAST', 'SOCU North East');

INSERT INTO groups (group_id, group_code, group_name)
VALUES (NEWID(), 'SOC_NORTH_WEST', 'SOCU North West');

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'SOC_RESTRICTED'),
        (select group_id from groups where group_code = 'SOC_NORTH_EAST'),
        1);

INSERT INTO group_assignable_role (role_id, group_id, automatic)
VALUES ((select role_id from roles where role_code = 'SOC_RESTRICTED'),
        (select group_id from groups where group_code = 'SOC_NORTH_WEST'),
        1);

-- Create the oauth service for Manage SOC cases in the development environment only, based on the presence of hmpps-auth dev instance.

insert into oauth_service (code, name, description, authorised_roles, url, enabled, email)
select 'SOC', 'Manage SOC cases', 'View and manage SOC cases', 'ROLE_SOC_CUSTODY,ROLE_SOC_COMMUNITY,ROLE_SOC_RESTRICTED', 'https://manage-soc-cases-dev.service.justice.gov.uk', 1, null
from oauth_service where code = 'DETAILS' and url like '%dev%';
