CREATE TABLE categories (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(7) NOT NULL DEFAULT 'BOTH' CHECK (type IN ('INCOME', 'EXPENSE', 'BOTH')),
    CONSTRAINT categories_owner_id_id_unique UNIQUE (owner_id, id)
);

CREATE INDEX categories_owner_name_idx ON categories (owner_id, name, id);

ALTER TABLE transactions ADD COLUMN category_id UUID;
ALTER TABLE transactions ADD CONSTRAINT transactions_owner_category_fk
    FOREIGN KEY (owner_id, category_id) REFERENCES categories (owner_id, id);
