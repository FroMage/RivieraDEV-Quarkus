-- Memoized rendered banner images, one per (talk|speaker|sponsor, language)
create table Banner (
    id bigint primary key,
    talk_id bigint references Talk(id) on delete cascade,
    speaker_id bigint references Speaker(id) on delete cascade,
    sponsor_id bigint references Sponsor(id) on delete cascade,
    language varchar(255) not null,
    etag varchar(255) not null,
    png oid,
    constraint banner_has_one_owner check (num_nonnulls(talk_id, speaker_id, sponsor_id) = 1)
);

create unique index banner_talk_language on Banner (talk_id, language);
create unique index banner_speaker_language on Banner (speaker_id, language);
create unique index banner_sponsor_language on Banner (sponsor_id, language);

create sequence Banner_SEQ start with 1 increment by 50;
