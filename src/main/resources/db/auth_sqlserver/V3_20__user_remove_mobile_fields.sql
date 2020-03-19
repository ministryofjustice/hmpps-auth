DECLARE @ConstraintName nvarchar(200);

SELECT @ConstraintName = Name FROM SYS.DEFAULT_CONSTRAINTS
WHERE PARENT_OBJECT_ID = OBJECT_ID('users')
AND PARENT_COLUMN_ID = (SELECT column_id FROM sys.columns
    WHERE NAME = N'mobile_verified'
    AND object_id = OBJECT_ID(N'users'));

IF @ConstraintName IS NOT NULL
    EXEC('ALTER TABLE users DROP CONSTRAINT ' + @ConstraintName)

alter table users
    drop column mobile;
alter table users
    drop column mobile_verified;
