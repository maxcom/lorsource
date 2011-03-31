alter table users add column karma int not null default 0;
alter table users add column karma_votes int not null default 0;

update users set karma = score/100 where score>0;
update users set karma_votes = (score-50) / 20 where score>50;

create table karma_voted (
	voter int not null references users(id),
	userid int not null references users(id)
);

grant all on karma_voted to linuxweb;
