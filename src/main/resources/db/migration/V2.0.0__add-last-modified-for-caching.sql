alter table Organiser add column lastUpdated timestamp(6);
alter table PreviousSpeaker add column lastUpdated timestamp(6);
alter table Speaker add column lastUpdated timestamp(6);
alter table Sponsor add column lastUpdated timestamp(6);

update Organiser set lastUpdated = current_timestamp;
update PreviousSpeaker set lastUpdated = current_timestamp;
update Speaker set lastUpdated = current_timestamp;
update Sponsor set lastUpdated = current_timestamp;
