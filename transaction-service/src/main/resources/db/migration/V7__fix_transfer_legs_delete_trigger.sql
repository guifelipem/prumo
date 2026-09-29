CREATE OR REPLACE FUNCTION check_transfer_legs() RETURNS TRIGGER AS $$
DECLARE
    checked_id UUID;
    transfer_row transfers%ROWTYPE;
    matching_count INTEGER;
BEGIN
    IF TG_TABLE_NAME = 'transfers' THEN
        IF TG_OP = 'DELETE' THEN checked_id := OLD.id;
        ELSE checked_id := NEW.id;
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        checked_id := OLD.transfer_id;
    ELSE
        checked_id := NEW.transfer_id;
    END IF;
    IF checked_id IS NULL THEN RETURN NULL; END IF;
    SELECT * INTO transfer_row FROM transfers WHERE id = checked_id;
    IF NOT FOUND THEN RETURN NULL; END IF;
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
