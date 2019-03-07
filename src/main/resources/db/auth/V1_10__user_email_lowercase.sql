update user_email
set email = lower(email)
where email != lower(email)
