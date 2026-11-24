-- Registar nacina placanja (plugin-ova) koje PSP podrzava.
CREATE TABLE payment_method (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(32)  NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL,
    service_url  VARCHAR(255),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Klijent PSP-a (npr. parking sistem, web shop).
CREATE TABLE merchant (
    id                UUID PRIMARY KEY,
    merchant_id       VARCHAR(32)  NOT NULL UNIQUE,
    name              VARCHAR(150) NOT NULL,
    email             VARCHAR(150) NOT NULL UNIQUE,
    keycloak_user_id  VARCHAR(64)  NOT NULL UNIQUE,
    api_key_hash      VARCHAR(64)  NOT NULL,
    success_url       VARCHAR(500) NOT NULL,
    failed_url        VARCHAR(500) NOT NULL,
    error_url         VARCHAR(500) NOT NULL,
    webhook_url       VARCHAR(500) NOT NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL
);

-- Pretplata klijenta na nacin placanja.
CREATE TABLE merchant_subscription (
    id                 BIGSERIAL PRIMARY KEY,
    merchant_id        UUID   NOT NULL REFERENCES merchant(id),
    payment_method_id  BIGINT NOT NULL REFERENCES payment_method(id),
    active             BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_merchant_method UNIQUE (merchant_id, payment_method_id)
);

INSERT INTO payment_method (code, name) VALUES
    ('CARD',   'Platna kartica'),
    ('QR',     'IPS QR kod'),
    ('PAYPAL', 'PayPal'),
    ('CRYPTO', 'Kriptovaluta');
