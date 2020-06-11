UPDATE oauth_client_details
SET additional_information = '{"jwtFields":"-name"}'
WHERE additional_information = '{"jwtFields":"-name,+user_name"}';
