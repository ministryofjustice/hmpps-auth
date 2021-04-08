alter table oauth_client_details
    add last_accessed DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP;
