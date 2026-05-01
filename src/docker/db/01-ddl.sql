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
