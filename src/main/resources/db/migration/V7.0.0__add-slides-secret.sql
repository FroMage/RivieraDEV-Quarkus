ALTER TABLE Talk ADD COLUMN slidesSecret VARCHAR(255);
UPDATE Talk SET slidesSecret = gen_random_uuid()::text;
