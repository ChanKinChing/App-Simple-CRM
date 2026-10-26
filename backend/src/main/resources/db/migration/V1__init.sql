CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(100) NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'SALES'))
);

CREATE TABLE customers (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    phone       VARCHAR(30),
    email       VARCHAR(120),
    company     VARCHAR(120),
    note        VARCHAR(1000),
    created_by  BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE follow_ups (
    id          BIGSERIAL     PRIMARY KEY,
    customer_id BIGINT        NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    content     VARCHAR(2000) NOT NULL,
    created_by  BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_name       ON customers (LOWER(name));
CREATE INDEX idx_customers_company    ON customers (LOWER(company));
CREATE INDEX idx_customers_created_at ON customers (created_at DESC);
CREATE INDEX idx_follow_ups_customer  ON follow_ups (customer_id, created_at DESC);
