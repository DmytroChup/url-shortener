ALTER TABLE urls
    ADD COLUMN user_id BIGINT;

CREATE INDEX idx_urls_user_id ON urls(user_id) where user_id IS NOT NULL;
