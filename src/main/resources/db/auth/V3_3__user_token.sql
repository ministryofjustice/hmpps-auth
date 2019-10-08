ALTER TABLE user_token
    ADD user_id UNIQUEIDENTIFIER;

ALTER TABLE user_token
    ADD CONSTRAINT user_token_user_id_fk FOREIGN KEY (user_id) REFERENCES users (user_id);
