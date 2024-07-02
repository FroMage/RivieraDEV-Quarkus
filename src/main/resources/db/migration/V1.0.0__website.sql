
    create sequence Configuration_SEQ start with 1 increment by 50;

    create sequence Organiser_SEQ start with 1 increment by 50;

    create sequence PreviousSpeaker_SEQ start with 1 increment by 50;

    create sequence PricePack_SEQ start with 1 increment by 50;

    create sequence PricePackDate_SEQ start with 1 increment by 50;

    create sequence Slot_SEQ start with 1 increment by 50;

    create sequence Speaker_SEQ start with 1 increment by 50;

    create sequence Sponsor_SEQ start with 1 increment by 50;

    create sequence Talk_SEQ start with 1 increment by 50;

    create sequence TalkTheme_SEQ start with 1 increment by 50;

    create sequence TalkType_SEQ start with 1 increment by 50;

    create sequence TemporarySlot_SEQ start with 1 increment by 50;

    create sequence Track_SEQ start with 1 increment by 50;

    create sequence user_table_SEQ start with 1 increment by 50;

    create table Configuration (
        id bigint not null,
        key varchar(255) check (key in ('GOOGLE_MAP_API_KEY','EVENT_START_DATE','EVENT_END_DATE','DISPLAY_FULL_SCHEDULE','DISPLAY_NEW_SPEAKERS','DISPLAY_TALKS','PROMOTED_PAGE','PROMOTED_PAGE_2','TICKETING_URL','TICKETING_OPEN','TICKETING_TRAINING_URL','TICKETING_TRAINING_OPEN','SPONSORING_LEAFLET_URL','CFP_OPEN','CFP_URL','CANCELLED_URL', 'FEEDBACK_URL')),
        value varchar(255),
        primary key (id)
    );

    create table Organiser (
        id bigint not null,
        biography varchar(32600),
        blogURL varchar(255),
        cfp boolean not null,
        company varchar(255),
        companyURL varchar(255),
        firstName varchar(255),
        lastName varchar(255),
        orga boolean not null,
        photo oid,
        title varchar(255),
        twitterAccount varchar(255),
        primary key (id)
    );

    create table PreviousSpeaker (
        id bigint not null,
        company varchar(255),
        firstName varchar(255),
        lastName varchar(255),
        photo oid,
        year integer,
        primary key (id)
    );

    create table PricePack (
        id bigint not null,
        blindBirdPrice integer,
        earlyBirdPrice integer,
        regularPrice integer,
        soldOut boolean,
        studentPrice integer,
        type varchar(255) check (type in ('DEEP_DIVE','CONF','COMBI')),
        primary key (id)
    );

    create table PricePackDate (
        id bigint not null,
        blindBirdEndDate timestamp(6),
        earlyBirdEndDate timestamp(6),
        regularEndDate timestamp(6),
        primary key (id)
    );

    create table Slot (
        id bigint not null,
        endDate timestamp(6),
        startDate timestamp(6),
        primary key (id)
    );

    create table Speaker (
        id bigint not null,
        biography varchar(32600),
        blogURL varchar(255),
        company varchar(255),
        companyURL varchar(255),
        email varchar(255),
        firstName varchar(255),
        importId varchar(255),
        lastName varchar(255),
        phone varchar(255),
        photo oid,
        star boolean not null,
        title varchar(255),
        twitterAccount varchar(255),
        primary key (id)
    );

    create table Sponsor (
        id bigint not null,
        about varchar(32600),
        aboutEN varchar(32600),
        company varchar(255),
        companyURL varchar(255),
        height integer,
        level varchar(255) check (level in ('Diamond','Platinum','Gold','Silver','Lunches','Party','Partner','PreviousYears','Schools')),
        linkedInAccount varchar(255),
        logo oid,
        otherURL varchar(255),
        twitterAccount varchar(255),
        width integer,
        primary key (id)
    );

    create table Talk (
        id bigint not null,
        descriptionEN varchar(32600),
        descriptionFR varchar(32600),
        importId varchar(255),
        isBreak varchar(255) check (isBreak in ('NotABreak','CofeeBreak','Breakfast','Lunch','Party')),
        isHiddenInTalksPage boolean not null,
        language varchar(255) check (language in ('EN','FR')),
        level varchar(255) check (level in ('Beginner','Intermediate','Advanced')),
        nbLikes integer,
        slidesUrl varchar(255),
        titleEN varchar(255),
        titleFR varchar(255),
        slot_id bigint,
        theme_id bigint,
        track_id bigint,
        type_id bigint,
        primary key (id)
    );

    create table talk_speaker (
        talk_id bigint not null,
        speakers_id bigint not null
    );

    create table TalkTheme (
        id bigint not null,
        color varchar(255) check (color in ('Blue','Yellow','Purple','Green','Orange','Red','Pink','Turquoise','Brown','Grey')),
        importId varchar(255),
        theme varchar(255),
        primary key (id)
    );

    create table TalkType (
        id bigint not null,
        importId varchar(255),
        typeEN varchar(255),
        typeFR varchar(255),
        primary key (id)
    );

    create table TemporarySlot (
        id bigint not null,
        endDate timestamp(6),
        isBreak varchar(255) check (isBreak in ('NotABreak','CofeeBreak','Breakfast','Lunch','Party')),
        labelEN varchar(255),
        labelFR varchar(255),
        startDate timestamp(6),
        primary key (id)
    );

    create table Track (
        id bigint not null,
        isJUDCon boolean not null,
        position integer not null,
        title varchar(255),
        primary key (id)
    );

    create table user_table (
        id bigint not null,
        firstName varchar(255),
        isBCrypt boolean,
        lastName varchar(255),
        password varchar(255),
        userName varchar(255),
        primary key (id)
    );

    alter table if exists Talk 
       add constraint FKojjqgj882m094k8krq50l6s9f 
       foreign key (slot_id) 
       references Slot;

    alter table if exists Talk 
       add constraint FKa52ovciu4texkr2loi3gq8xns 
       foreign key (theme_id) 
       references TalkTheme;

    alter table if exists Talk 
       add constraint FKab1vdtaevhgbtbu8cdu4en2wl 
       foreign key (track_id) 
       references Track;

    alter table if exists Talk 
       add constraint FKfkv0wo4nkx7d0qvqno8p86el4 
       foreign key (type_id) 
       references TalkType;

    alter table if exists talk_speaker 
       add constraint FKt0drkd7rfm8csipawpfa8pch2 
       foreign key (speakers_id) 
       references Speaker;

    alter table if exists talk_speaker 
       add constraint FKm4spa6xc9qpx4fqgj92del8f4 
       foreign key (talk_id) 
       references Talk;
