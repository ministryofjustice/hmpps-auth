UPDATE users
set source = CASE
                 WHEN master = 1 THEN 'auth'
                 ELSE 'nomis'
    END;

ALTER TABLE users
ALTER
COLUMN source VARCHAR(50) NOT NULL;
