DROP TABLE user_email_groups;
DROP TABLE user_email_roles;
DROP TABLE person;
DROP TABLE user_email;
ALTER TABLE user_token
    DROP COLUMN username;
