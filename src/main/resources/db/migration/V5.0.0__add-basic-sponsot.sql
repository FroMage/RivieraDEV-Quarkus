alter table sponsor drop constraint sponsor_level_check;
alter table sponsor add constraint sponsor_level_check check (level in ('Diamond','Platinum','Gold','Silver','Lunches','Party','Partner','Basic','PreviousYears','Schools'));
