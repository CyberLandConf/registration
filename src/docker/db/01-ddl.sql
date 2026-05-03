CREATE TABLE Event
(
    uid         VARCHAR(255) NOT NULL,
    eventId     VARCHAR(255),
    summary     VARCHAR(255),
    title       VARCHAR(255),
    description TEXT,
    speaker     VARCHAR(255),
    twitter     VARCHAR(255),
    location    VARCHAR(255),
    url         VARCHAR(255),
    startDate   TIMESTAMP WITHOUT TIME ZONE,
    endDate     TIMESTAMP WITHOUT TIME ZONE,
    timezone    VARCHAR(255),
    CONSTRAINT pk_event PRIMARY KEY (uid)
);

CREATE TABLE Registration
(
    id             VARCHAR(255) NOT NULL,
    eventId        VARCHAR(255),
    name           VARCHAR(255),
    email          VARCHAR(255),
    pub            BOOLEAN      NOT NULL,
    waitlist       BOOLEAN      NOT NULL,
    privacy        BOOLEAN      NOT NULL,
    videoRecording BOOLEAN      NOT NULL,
    remote         BOOLEAN      NOT NULL,
    created        TIMESTAMP WITHOUT TIME ZONE,
    ttl            BIGINT,
    CONSTRAINT pk_registration PRIMARY KEY (id)
);
