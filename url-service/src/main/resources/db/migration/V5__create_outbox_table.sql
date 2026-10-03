CREATE TABLE outbox_events
(
    id               UUID                       NOT NULL,
    aggregate_type   VARCHAR(50)                NOT NULL,
    aggregate_id     VARCHAR(50)                NOT NULL,
    event_type       VARCHAR(100)               NOT NULL,
    payload          TEXT                       NOT NULL,
    status           VARCHAR(20)                NOT NULL DEFAULT 'PENDING',
    retry_count      INT                        NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE   NOT NULL DEFAULT now(),
    sent_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_pending ON outbox_events(created_at) WHERE status = 'PENDING';