alter table oauth_service
    add email VARCHAR(240);

update oauth_service
set email = 'hdcdigitalservice@digital.justice.gov.uk'
where code = 'HDC';

update oauth_service
set email = 'feedback@digital.justice.gov.uk'
where code = 'NOMIS';

update oauth_service
set email = 'categorisation@justice.gov.uk'
where code = 'CATTOOL';

update oauth_service
set email = 'bookasecuremove@digital.justice.gov.uk'
where code = 'BOOK_MOVE';

update oauth_service
set email = 'moic@digital.justice.gov.uk'
where code = 'POM';
