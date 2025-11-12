
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_positive_amount
    CHECK (amount > 0);

ALTER TABLE wallets ADD CONSTRAINT chk_wallets_positive_balance
    CHECK (balance >= 0);

CREATE OR REPLACE FUNCTION update_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_updated_at
    BEFORE UPDATE ON wallets
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at();