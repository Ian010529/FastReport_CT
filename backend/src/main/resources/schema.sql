-- Customers table
CREATE TABLE IF NOT EXISTS customers (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     VARCHAR(8) UNIQUE NOT NULL,
    customer_name   VARCHAR(100) NOT NULL,
    national_id     VARCHAR(18) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Managers table
CREATE TABLE IF NOT EXISTS managers (
    id              BIGSERIAL PRIMARY KEY,
    manager_id      VARCHAR(6) UNIQUE NOT NULL,
    manager_name    VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Reports table
CREATE TABLE IF NOT EXISTS reports (
    id                  BIGSERIAL PRIMARY KEY,
    customer_id         BIGINT NOT NULL REFERENCES customers(id),
    manager_id          BIGINT NOT NULL REFERENCES managers(id),
    service_code        VARCHAR(50) NOT NULL,
    current_plan        VARCHAR(100) NOT NULL,
    additional_services TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'pending',
    report_content      TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Spending history table
CREATE TABLE IF NOT EXISTS spending_history (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT NOT NULL REFERENCES reports(id),
    month           DATE NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL
);

-- Complaints table
CREATE TABLE IF NOT EXISTS complaints (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT NOT NULL REFERENCES reports(id),
    complaint_date  DATE NOT NULL,
    description     TEXT NOT NULL
);

-- Network quality table
CREATE TABLE IF NOT EXISTS network_quality (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT NOT NULL REFERENCES reports(id),
    metric          VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL
);

-- API Gateway WebSocket subscriptions for AWS runtime
CREATE TABLE IF NOT EXISTS report_ws_subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    connection_id   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP NOT NULL,
    UNIQUE (report_id, connection_id)
);

CREATE INDEX IF NOT EXISTS idx_report_ws_subscriptions_report_id
    ON report_ws_subscriptions(report_id);

CREATE INDEX IF NOT EXISTS idx_report_ws_subscriptions_connection_id
    ON report_ws_subscriptions(connection_id);
