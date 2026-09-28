ALTER TABLE transactions ADD COLUMN created_at TIMESTAMPTZ;
UPDATE transactions SET created_at = occurred_at;
ALTER TABLE transactions ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE transactions ALTER COLUMN created_at SET NOT NULL;
