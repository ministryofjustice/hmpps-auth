update oauth_service
set name = 'Manage your account details', description = ''
where code = 'DETAILS';

update oauth_service
set name = 'Manage user accounts', description = ''
where code = 'USERADMIN';

update oauth_service
set name = 'Digital Categorisation Service'
where code = 'CATTOOL';
