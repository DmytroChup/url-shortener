ALTER TABLE url_clicks
    ADD COLUMN country_code      VARCHAR(10),
    ADD COLUMN device_type       VARCHAR(20),
    ADD COLUMN browser           VARCHAR(50),
    ADD COLUMN operating_system  VARCHAR(50);

