-- MERCHANT_ID koji je Acquirer banka dodelila prodavcu (tabela 2); potreban za placanje karticom/QR kodom.
ALTER TABLE merchant ADD COLUMN acquirer_merchant_id VARCHAR(32);

-- STAN (System Trace Audit Number): 6-cifreni broj za pracenje transakcije izmedju PSP-a i banke.
CREATE SEQUENCE stan_seq MINVALUE 1 MAXVALUE 999999 CYCLE;

CREATE TABLE payment_transaction (
    id                     UUID PRIMARY KEY,
    merchant_id            UUID           NOT NULL REFERENCES merchant(id),
    merchant_order_id      VARCHAR(64)    NOT NULL,
    merchant_timestamp     TIMESTAMPTZ    NOT NULL,
    amount                 NUMERIC(17, 2) NOT NULL CHECK (amount > 0),
    currency               VARCHAR(3)        NOT NULL,
    status                 VARCHAR(16)    NOT NULL,
    payment_method         VARCHAR(32),
    stan                   VARCHAR(6),
    psp_timestamp          TIMESTAMPTZ,
    external_payment_id    VARCHAR(64),
    global_transaction_id  VARCHAR(64),
    acquirer_timestamp     TIMESTAMPTZ,
    expires_at             TIMESTAMPTZ    NOT NULL,
    created_at             TIMESTAMPTZ    NOT NULL,
    updated_at             TIMESTAMPTZ    NOT NULL,
    version                BIGINT         NOT NULL DEFAULT 0,
    -- Isti nalog prodavca ne moze da napravi dve transakcije (zastita od dvostrukog slanja)
    CONSTRAINT uq_merchant_order UNIQUE (merchant_id, merchant_order_id)
);

CREATE UNIQUE INDEX uq_external_payment ON payment_transaction (payment_method, external_payment_id);
