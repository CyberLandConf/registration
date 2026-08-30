-- Optional, tenant-owned stylesheet for the participant pages (registration, deregistration, webinar).
-- TEXT rather than VARCHAR(255) like the other columns: this holds a whole stylesheet, not a URL.
ALTER TABLE tenant ADD COLUMN css TEXT;
