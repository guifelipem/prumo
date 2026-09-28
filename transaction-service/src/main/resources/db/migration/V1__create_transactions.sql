CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    account_id UUID NOT NULL,
    type VARCHAR(7) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    description VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX transactions_owner_account_idx ON transactions (owner_id, account_id);
