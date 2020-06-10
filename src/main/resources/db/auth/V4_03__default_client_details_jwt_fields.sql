UPDATE oauth_client_details
SET additional_information = '{"jwtFields":"-name,+user_name"}'
WHERE additional_information = '{}';

UPDATE oauth_client_details
SET additional_information = '{}'
WHERE authorities = 'client_credentials';
