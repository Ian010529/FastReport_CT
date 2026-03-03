CREATE TABLE IF NOT EXISTS reports (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     VARCHAR(8)   NOT NULL,
    customer_name   VARCHAR(100) NOT NULL,
    national_id     VARCHAR(18)  NOT NULL,
    manager_name    VARCHAR(100) NOT NULL,
    manager_id      VARCHAR(6)   NOT NULL,
    service_code    VARCHAR(50)  NOT NULL,
    current_plan    VARCHAR(100) NOT NULL,
    additional_services TEXT,
    spending_last6  TEXT,
    complaint_history   TEXT,
    network_quality     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    report_content  TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
