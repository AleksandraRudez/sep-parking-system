CREATE TABLE bank_account (
    id                 BIGSERIAL PRIMARY KEY,
    account_number     VARCHAR(32)    NOT NULL UNIQUE,
    owner_name         VARCHAR(150)   NOT NULL,
    currency           VARCHAR(3)        NOT NULL,
    -- Raspolozivo stanje; rezervacija ga umanjuje i povecava reserved_balance
    available_balance  NUMERIC(17, 2) NOT NULL CHECK (available_balance >= 0),
    reserved_balance   NUMERIC(17, 2) NOT NULL DEFAULT 0 CHECK (reserved_balance >= 0)
);

-- Kartica kupca. PAN i CVV se ne cuvaju u citljivom obliku, vec kao HMAC (PCI DSS zahtev 3).
CREATE TABLE bank_card (
    id             BIGSERIAL PRIMARY KEY,
    pan_hmac       VARCHAR(64)     NOT NULL UNIQUE,
    pan_last4      VARCHAR(4)      NOT NULL,
    brand          VARCHAR(16)  NOT NULL,
    holder_name    VARCHAR(100) NOT NULL,
    expiry_month   SMALLINT     NOT NULL,
    expiry_year    SMALLINT     NOT NULL,
    cvv_hmac       VARCHAR(64)     NOT NULL,
    account_id     BIGINT       NOT NULL REFERENCES bank_account(id)
);

-- Prodavac sa kojim banka ima ugovor (MERCHANT_ID iz tabele 2).
CREATE TABLE acquirer_merchant (
    id           BIGSERIAL PRIMARY KEY,
    merchant_id  VARCHAR(32)  NOT NULL UNIQUE,
    name         VARCHAR(150) NOT NULL,
    account_id   BIGINT       NOT NULL REFERENCES bank_account(id)
);

-- Jedna forma za placanje (PAYMENT_ID / PAYMENT_URL), vezana za tacno jednu transakciju.
CREATE TABLE payment_session (
    id                     UUID PRIMARY KEY,
    merchant_id            BIGINT         NOT NULL REFERENCES acquirer_merchant(id),
    amount                 NUMERIC(17, 2) NOT NULL CHECK (amount > 0),
    currency               VARCHAR(3)        NOT NULL,
    stan                   VARCHAR(6)        NOT NULL,
    psp_timestamp          TIMESTAMPTZ    NOT NULL,
    status                 VARCHAR(16)    NOT NULL,
    expires_at             TIMESTAMPTZ    NOT NULL,
    payer_account_id       BIGINT REFERENCES bank_account(id),
    card_last4             VARCHAR(4),
    debited_amount         NUMERIC(17, 2),
    debited_currency       VARCHAR(3),
    failure_reason         VARCHAR(200),
    global_transaction_id  VARCHAR(64) UNIQUE,
    acquirer_timestamp     TIMESTAMPTZ,
    created_at             TIMESTAMPTZ    NOT NULL,
    updated_at             TIMESTAMPTZ    NOT NULL,
    -- MERCHANT_ID + STAN + PSP_TIMESTAMP jednoznacno odredjuju transakciju
    CONSTRAINT uq_session_trace UNIQUE (merchant_id, stan, psp_timestamp)
);
