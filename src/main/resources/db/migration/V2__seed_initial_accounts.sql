-- =========================================================================
-- V2__seed_initial_accounts.sql: Seed Initial Test Bank Accounts
-- =========================================================================

INSERT INTO accounts (account_number, account_holder_name, balance, currency, version, created_at, updated_at)
VALUES 
    ('1000000001', 'Emmanuel Ugwueze', 500000.0000, 'NGN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('1000000002', 'Ekene iloezumma', 250000.0000, 'NGN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('1000000003', 'Ikemefuna Nwodo', 50.0000, 'NGN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('1000000004', 'DotLabs Treasury', 10000000.0000, 'NGN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
