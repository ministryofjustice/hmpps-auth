create table user_contact
(
    user_id  UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT user_contact_user_id_fk REFERENCES users (user_id),
    type     VARCHAR(20)      NOT NULL,
    value    VARCHAR(240)     NOT NULL,
    verified BIT              NOT NULL DEFAULT 0
);

alter table user_contact
    ADD CONSTRAINT user_contact_type_uk UNIQUE (user_id, type);
