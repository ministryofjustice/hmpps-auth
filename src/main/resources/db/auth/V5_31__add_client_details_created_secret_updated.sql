alter table oauth_client_details
    add created DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table oauth_client_details
    add secret_updated DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP;
