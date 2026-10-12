alter table Talk add column lastUpdated timestamp(6);

update Talk set lastUpdated = current_timestamp;
