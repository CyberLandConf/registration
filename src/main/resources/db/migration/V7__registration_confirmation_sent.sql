ALTER TABLE registration ADD COLUMN confirmationSentAt TIMESTAMP WITHOUT TIME ZONE;

-- Rows that already exist were written while the mail was still sent inside the transaction:
-- a failed send rolled the registration back, so every persisted row implies a delivered mail.
-- Backfilling with the creation date keeps them out of the "not delivered" marker in the admin UI.
UPDATE registration SET confirmationSentAt = created;
