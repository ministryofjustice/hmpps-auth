ALTER TABLE users
    ADD mobile VARCHAR(20);
ALTER TABLE users
    ADD mobile_verified BIT NOT NULL DEFAULT 0;
