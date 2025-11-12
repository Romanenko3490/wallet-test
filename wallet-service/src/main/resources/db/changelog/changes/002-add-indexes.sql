CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);


CREATE INDEX idx_transactions_operation_track_id ON transactions(operation_track_id);

CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);

CREATE INDEX idx_wallets_updated_at ON wallets(updated_at DESC);