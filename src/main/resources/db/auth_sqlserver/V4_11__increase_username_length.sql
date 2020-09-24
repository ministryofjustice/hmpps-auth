DECLARE @ConstraintName nvarchar(200);

SELECT @ConstraintName = name FROM SYS.KEY_CONSTRAINTS
WHERE parent_object_id = OBJECT_ID('user_retries')
AND [type] = 'PK';

IF @ConstraintName IS NOT NULL
    EXEC('ALTER TABLE user_retries DROP CONSTRAINT ' + @ConstraintName);

ALTER TABLE user_retries ALTER COLUMN username varchar(37) NOT NULL;

IF @ConstraintName IS NOT NULL
    ALTER TABLE user_retries ADD CONSTRAINT user_retries_pk PRIMARY KEY (username);

ALTER TABLE users DROP CONSTRAINT username_uk;
ALTER TABLE users ALTER COLUMN username varchar(37) NOT NULL;
ALTER TABLE users ADD CONSTRAINT username_uk UNIQUE (username);
