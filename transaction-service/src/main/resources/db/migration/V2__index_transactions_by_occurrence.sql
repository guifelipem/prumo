CREATE INDEX transactions_owner_account_occurrence_idx
    ON transactions (owner_id, account_id, occurred_at DESC, id DESC);
