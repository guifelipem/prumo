CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    description VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT transfers_distinct_accounts CHECK (source_account_id <> destination_account_id),
    CONSTRAINT transfers_owner_id_id_unique UNIQUE (owner_id, id)
);

ALTER TABLE transactions ADD COLUMN transfer_id UUID;
ALTER TABLE transactions ADD CONSTRAINT transactions_owner_transfer_fk
    FOREIGN KEY (owner_id, transfer_id) REFERENCES transfers (owner_id, id);
ALTER TABLE transactions ADD CONSTRAINT transactions_transfer_has_no_category
    CHECK (transfer_id IS NULL OR category_id IS NULL);
CREATE UNIQUE INDEX transactions_transfer_type_unique ON transactions (transfer_id, type)
    WHERE transfer_id IS NOT NULL;

CREATE FUNCTION check_transfer_legs() RETURNS TRIGGER AS $$
DECLARE
    checked_id UUID;
    transfer_row transfers%ROWTYPE;
    matching_count INTEGER;
BEGIN
    IF TG_TABLE_NAME = 'transfers' THEN
        checked_id := COALESCE(NEW.id, OLD.id);
    ELSE
        checked_id := COALESCE(NEW.transfer_id, OLD.transfer_id);
    END IF;
    IF checked_id IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT * INTO transfer_row FROM transfers WHERE id = checked_id;
    IF NOT FOUND THEN
        RETURN NULL;
    END IF;
    SELECT COUNT(*) INTO matching_count FROM transactions
    WHERE transfer_id = checked_id AND owner_id = transfer_row.owner_id
      AND amount = transfer_row.amount AND description = transfer_row.description
      AND occurred_at = transfer_row.occurred_at AND category_id IS NULL
      AND ((type = 'EXPENSE' AND account_id = transfer_row.source_account_id)
        OR (type = 'INCOME' AND account_id = transfer_row.destination_account_id));
    IF matching_count <> 2 THEN
        RAISE EXCEPTION 'transfer % requires two matching legs', checked_id USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER transfers_legs_check
    AFTER INSERT OR UPDATE ON transfers DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_transfer_legs();
CREATE CONSTRAINT TRIGGER transactions_transfer_legs_check
    AFTER INSERT OR UPDATE OR DELETE ON transactions DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_transfer_legs();
