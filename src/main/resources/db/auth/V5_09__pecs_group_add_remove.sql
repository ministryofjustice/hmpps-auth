insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_BARRMC', 'PECS Court Barrow-in-Furness Magistrates Court');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_BARRMC'),
        1);


insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_KLYNMC', 'PECS Court King''s Lynn Magistrates Court');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_KLYNMC'),
        1);


insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_STOTMC', 'PECS Court Stoke-on-Trent Magistrates Court');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_STOTMC'),
        1);


insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_CRNHMC', 'PECS Court Caernarfon Magistrates Court');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_CRNHMC'),
        1);


insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_CRNRCC', 'PECS Court Caernarfon Crown Court');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_CRNRCC'),
        1);


insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_STAFMC', 'PECS Court Stafford Magistrates Court');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_STAFMC'),
        1);


insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_WINCMC', 'PECS Court Winchester Magistrates Court');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_WINCMC'),
        1);


insert into groups (group_id, group_code, group_name)
    values (NEWID(), 'PECS_BRGEMC', 'PECS Court Brighton Magistrates Court (Edward St)');

insert into group_assignable_role (role_id, group_id, automatic)
    values ((select role_id from roles where role_code = 'PECS_COURT'),
        (select group_id from groups where group_code = 'PECS_BRGEMC'),
        1);


delete
from user_group
where group_id in
      (select group_id
       from groups
       where group_code in
             ('PECS_BARRBC','PECS_BOLTBC', 'PECS_BRIGMC', 'PECS_CAERBC',
              'PECS_EXETBC', 'PECS_HAVEBC', 'PECS_KINGBC', 'PECS_MERTBC',
              'PECS_MOLDBC', 'PECS_NEWPBC', 'PECS_SOUTBC', 'PECS_STOKBC',
              'PECS_WARRCC', 'PECS_HUNTBC', 'PECS_SALIBC', 'PECS_WARWBC',
              'PECS_HGHBMC')
      );

delete
from group_assignable_role
where group_id in
      (select group_id
       from groups
       where group_code in
             ('PECS_BARRBC','PECS_BOLTBC', 'PECS_BRIGMC', 'PECS_CAERBC',
              'PECS_EXETBC', 'PECS_HAVEBC', 'PECS_KINGBC', 'PECS_MERTBC',
              'PECS_MOLDBC', 'PECS_NEWPBC', 'PECS_SOUTBC', 'PECS_STOKBC',
              'PECS_WARRCC', 'PECS_HUNTBC', 'PECS_SALIBC', 'PECS_WARWBC',
              'PECS_HGHBMC')
      );

delete
from child_group
where group_id in
        (select group_id
        from groups
        where group_code in
        ('PECS_BARRBC','PECS_BOLTBC', 'PECS_BRIGMC', 'PECS_CAERBC',
             'PECS_EXETBC', 'PECS_HAVEBC', 'PECS_KINGBC', 'PECS_MERTBC',
              'PECS_MOLDBC', 'PECS_NEWPBC', 'PECS_SOUTBC', 'PECS_STOKBC',
              'PECS_WARRCC', 'PECS_HUNTBC', 'PECS_SALIBC', 'PECS_WARWBC',
               'PECS_HGHBMC') );

delete
from groups
where group_code in
      ('PECS_BARRBC','PECS_BOLTBC', 'PECS_BRIGMC', 'PECS_CAERBC',
       'PECS_EXETBC', 'PECS_HAVEBC', 'PECS_KINGBC', 'PECS_MERTBC',
       'PECS_MOLDBC', 'PECS_NEWPBC', 'PECS_SOUTBC', 'PECS_STOKBC',
       'PECS_WARRCC', 'PECS_HUNTBC', 'PECS_SALIBC', 'PECS_WARWBC',
       'PECS_HGHBMC');
