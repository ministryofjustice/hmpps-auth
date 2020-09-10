delete
from user_token
where user_id in
      (select user_id from user_token group by user_id, token_type having count(*) > 1);

ALTER TABLE user_token
    ADD CONSTRAINT user_token_user_id_token_type_uk UNIQUE (user_id, token_type);
