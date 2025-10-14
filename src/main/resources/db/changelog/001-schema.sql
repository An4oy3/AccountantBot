-- liquibase formatted sql
--changeset system:001-create-schema context:ddl

-- ============================================================================
-- 001: создание всех таблиц, индексов и ограничений для JPA сущностей
-- Содержит: users, accounts, categories, merchants, budgets, transactions, transaction_splits, attachments
-- ============================================================================

-- USERS
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    user_name   VARCHAR(64),
    first_name  VARCHAR(128),
    last_name   VARCHAR(128),
    chat_id     BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_username UNIQUE (user_name),
    CONSTRAINT uk_users_chat_id UNIQUE (chat_id)
);

-- ACCOUNTS (в owner_chat_id ссылаемся на users.chat_id)
CREATE TABLE IF NOT EXISTS accounts (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32)  NOT NULL,
    currency        CHAR(3)      NOT NULL DEFAULT 'PLN',
    owner_chat_id   BIGINT       NOT NULL,
    archived        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_accounts_owner ON accounts(owner_chat_id);
ALTER TABLE accounts
    ADD CONSTRAINT fk_account_owner_chat
        FOREIGN KEY (owner_chat_id) REFERENCES users(chat_id) ON DELETE CASCADE;

-- CATEGORIES
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    owner_id    BIGINT NULL,
    type        VARCHAR(16) NOT NULL,
    archived    BOOLEAN NOT NULL DEFAULT FALSE,
    depth       INT NOT NULL DEFAULT 0,
    parent_id   BIGINT,
    usage_count INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_category_owner_name_type UNIQUE (owner_id, name, type)
);
CREATE INDEX IF NOT EXISTS idx_category_owner ON categories(owner_id);
ALTER TABLE categories
    ADD CONSTRAINT fk_category_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
-- Частичный уникальный индекс для глобальных категорий (owner_id IS NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_global_category_name_type ON categories (name, type) WHERE owner_id IS NULL;

-- MERCHANTS
CREATE TABLE IF NOT EXISTS merchants (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NULL,
    name            VARCHAR(256) NOT NULL,
    normalized_name VARCHAR(256),
    external_ref    VARCHAR(128),
    archived        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_merchant_owner_name UNIQUE (owner_id, name)
);
CREATE INDEX IF NOT EXISTS idx_merchant_owner ON merchants(owner_id);
CREATE INDEX IF NOT EXISTS idx_merchant_normalized ON merchants(normalized_name);
ALTER TABLE merchants
    ADD CONSTRAINT fk_merchant_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;

-- BUDGETS
CREATE TABLE IF NOT EXISTS budgets (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL,
    category_id     BIGINT NOT NULL,
    period_type     VARCHAR(16) NOT NULL,
    start_date      DATE NOT NULL,
    amount_limit    NUMERIC(19,4) NOT NULL,
    spent_amount    NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        CHAR(3) NOT NULL,
    note            VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_budget_owner_category_period_start UNIQUE (owner_id, category_id, period_type, start_date)
);
CREATE INDEX IF NOT EXISTS idx_budget_owner ON budgets(owner_id);
CREATE INDEX IF NOT EXISTS idx_budget_category ON budgets(category_id);
CREATE INDEX IF NOT EXISTS idx_budget_period ON budgets(period_type, start_date);
ALTER TABLE budgets
    ADD CONSTRAINT fk_budget_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE budgets
    ADD CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE;

-- TRANSACTIONS
CREATE TABLE IF NOT EXISTS transactions (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL,
    type            VARCHAR(16) NOT NULL,
    status          VARCHAR(16) NOT NULL,
    account_id      BIGINT NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    currency        CHAR(3) NOT NULL,
    exchange_rate   NUMERIC(19,8),
    description     VARCHAR(512),
    note            VARCHAR(1024),
    operation_time  TIMESTAMP NOT NULL DEFAULT NOW(),
    posted_time     TIMESTAMP,
    external_ref    VARCHAR(128),
    category_id     BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_trx_account ON transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_trx_owner ON transactions(owner_id);
CREATE INDEX IF NOT EXISTS idx_trx_status ON transactions(status);
ALTER TABLE transactions
    ADD CONSTRAINT fk_trx_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE transactions
    ADD CONSTRAINT fk_trx_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE;
ALTER TABLE transactions
    ADD CONSTRAINT fk_trx_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- TRANSACTION_SPLITS (пока опционально, но создаём)
CREATE TABLE IF NOT EXISTS transaction_splits (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT NOT NULL,
    category_id     BIGINT,
    amount          NUMERIC(19,4) NOT NULL,
    percentage      NUMERIC(5,2),
    note            VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_split_trx ON transaction_splits(transaction_id);
CREATE INDEX IF NOT EXISTS idx_split_category ON transaction_splits(category_id);
ALTER TABLE transaction_splits
    ADD CONSTRAINT fk_split_trx FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE;
ALTER TABLE transaction_splits
    ADD CONSTRAINT fk_split_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- ATTACHMENTS
CREATE TABLE IF NOT EXISTS attachments (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT,
    file_name       VARCHAR(256),
    content_type    VARCHAR(128),
    size_bytes      BIGINT,
    checksum        VARCHAR(64),
    storage_path    VARCHAR(512),
    external_url    VARCHAR(1024),
    taken_at        TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_attachment_trx ON attachments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_attachment_checksum ON attachments(checksum);
ALTER TABLE attachments
    ADD CONSTRAINT fk_attachment_trx FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE;

--rollback DROP TABLE IF EXISTS attachments CASCADE;
--rollback DROP TABLE IF EXISTS transaction_splits CASCADE;
--rollback DROP TABLE IF EXISTS transactions CASCADE;
--rollback DROP TABLE IF EXISTS budgets CASCADE;
--rollback DROP TABLE IF EXISTS merchants CASCADE;
--rollback DROP TABLE IF EXISTS categories CASCADE;
--rollback DROP TABLE IF EXISTS accounts CASCADE;
--rollback DROP TABLE IF EXISTS users CASCADE;

