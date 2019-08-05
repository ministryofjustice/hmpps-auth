alter table user_email
    add last_logged_in DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP;

create index user_email_last_logged_in_enabled_idx on user_email (last_logged_in, enabled);

