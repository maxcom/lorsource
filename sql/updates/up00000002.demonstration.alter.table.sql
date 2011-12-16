-- комментарий с пояснением

alter table updates_test add column some_str varchar(10) default 'test';

insert into updates_test (id) values (123);

insert into db_updates (id) values ('00000002');

