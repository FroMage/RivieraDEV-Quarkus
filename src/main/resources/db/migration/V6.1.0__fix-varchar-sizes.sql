CREATE TABLE bufferpost (
    id BIGINT PRIMARY KEY,
    talk_id BIGINT UNIQUE REFERENCES talk(id),
    error VARCHAR(255),
    twitterPostId VARCHAR(255),
    blueskyPostId VARCHAR(255),
    linkedInPostId VARCHAR(255),
    scheduledDate TIMESTAMP
);

alter table if exists Organiser alter column biography set data type varchar(10000);
alter table if exists Speaker alter column biography set data type varchar(10000);
alter table if exists Sponsor alter column about set data type varchar(10000);
alter table if exists Sponsor alter column aboutEN set data type varchar(10000);
alter table if exists Talk alter column descriptionEN set data type varchar(10000);
alter table if exists Talk alter column descriptionFR set data type varchar(10000);
create sequence BufferPost_SEQ start with 1 increment by 50;
