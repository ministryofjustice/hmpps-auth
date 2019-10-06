ALTER TABLE user_token
    ADD user_id UNIQUEIDENTIFIER
        CONSTRAINT user_token_user_id_fk REFERENCES users (user_id);

-- copy data over to new field
UPDATE user_token ut
SET user_id = (select user_id FROM users u where u.username = ut.username);

-- change user_id now to be not nullable
ALTER TABLE user_token
    MODIFY user_id UNIQUEIDENTIFIER NOT NULL;

-- and username now to be nullable
ALTER TABLE user_token
    MODIFY username NULL;
