insert into user_contact (user_id, type, value, verified)
    select user_id, 'MOBILE_PHONE', mobile, mobile_verified from users where mobile is not null;
