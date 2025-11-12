CREATE TABLE IF NOT EXISTS wallets (
                         id UUID PRIMARY KEY,
                         balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
                         currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
                         version BIGINT NOT NULL DEFAULT 0,
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transactions (
                              id UUID PRIMARY KEY,
                              wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
                              operation_type VARCHAR(10) NOT NULL CHECK (operation_type IN ('DEPOSIT', 'WITHDRAW')),
                              amount BIGINT NOT NULL CHECK (amount > 0),
                              previous_balance BIGINT NOT NULL,
                              new_balance BIGINT NOT NULL,
                              operation_track_id UUID NOT NULL UNIQUE,
                              created_at TIMESTAMP NOT NULL DEFAULT NOW()
);