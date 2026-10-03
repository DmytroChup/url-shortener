ALTER TABLE url_clicks
    ADD COLUMN event_id UUID;

UPDATE url_clicks
SET event_id = gen_random_uuid()
WHERE event_id IS NULL;

ALTER TABLE url_clicks
    ALTER COLUMN event_id SET NOT NULL;

CREATE UNIQUE INDEX idx_url_clicks_event_id ON url_clicks (event_id);