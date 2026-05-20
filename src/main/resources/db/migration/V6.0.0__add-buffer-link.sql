CREATE TABLE bufferpost (
    id BIGINT PRIMARY KEY,
    talk_id BIGINT UNIQUE REFERENCES talk(id),
    sponsor_id BIGINT UNIQUE REFERENCES sponsor(id),
    error VARCHAR(10000),
    twitterPostId VARCHAR(255),
    blueskyPostId VARCHAR(255),
    linkedInPostId VARCHAR(255),
    scheduledDate TIMESTAMP
);

create sequence BufferPost_SEQ start with 1 increment by 50;
