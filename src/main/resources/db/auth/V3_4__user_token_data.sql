-- copy data over to new field
UPDATE user_token
SET user_id = (select u.user_id FROM users u where u.username = user_token.username);

-- change user_id now to be not nullable
ALTER TABLE user_token ALTER COLUMN user_id UNIQUEIDENTIFIER NOT NULL;

-- and username now to be nullable
DROP INDEX user_token_username_idx ON user_token;

ALTER TABLE user_token ALTER COLUMN username varchar(30) NULL;
