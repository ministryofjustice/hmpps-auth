alter table user_email
  add password_expiry DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
