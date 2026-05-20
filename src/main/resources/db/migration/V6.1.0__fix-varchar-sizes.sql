alter table if exists Organiser alter column biography set data type varchar(10000);
alter table if exists Speaker alter column biography set data type varchar(10000);
alter table if exists Sponsor alter column about set data type varchar(10000);
alter table if exists Sponsor alter column aboutEN set data type varchar(10000);
alter table if exists Talk alter column descriptionEN set data type varchar(10000);
alter table if exists Talk alter column descriptionFR set data type varchar(10000);
