
-- customers

CREATE TABLE customers (
    customer_id   INT AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150),
    phone         VARCHAR(20),
    address       TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- accounts

CREATE TABLE accounts (
    account_id      INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT NOT NULL,
    account_number  VARCHAR(50) NOT NULL UNIQUE,
    account_type    ENUM('CHECKING', 'SAVINGS', 'CREDIT') NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    status          ENUM('ACTIVE', 'CLOSED', 'FROZEN') NOT NULL DEFAULT 'ACTIVE',
    opened_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE CASCADE
);


-- payees

CREATE TABLE payees (
    payee_id          INT AUTO_INCREMENT PRIMARY KEY,
    payee_name        VARCHAR(150) NOT NULL,
    payee_identifier  VARCHAR(150) NOT NULL
);


-- transactions

CREATE TABLE transactions (
    transaction_id        INT AUTO_INCREMENT PRIMARY KEY,
    account_id            INT NOT NULL,
    payee_id              INT NOT NULL,
    amount                DECIMAL(15,2) NOT NULL,
    currency              VARCHAR(3) NOT NULL DEFAULT 'USD',
    type                  ENUM('DEBIT', 'CREDIT') NOT NULL,
    status                ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    description           VARCHAR(255),
    merchant_category     VARCHAR(100),
    location              VARCHAR(150),
    transaction_timestamp TIMESTAMP NOT NULL,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(account_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_transactions_payee
        FOREIGN KEY (payee_id) REFERENCES payees(payee_id)
        ON DELETE RESTRICT,
    INDEX idx_txn_account_timestamp (account_id, transaction_timestamp),
    INDEX idx_txn_payee (payee_id)
);


-- transaction_queue_status

CREATE TABLE transaction_queue_status (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id    INT NOT NULL,
    queue_status      ENUM('PENDING', 'PROCESSING', 'EVALUATED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    picked_up_at      TIMESTAMP NULL,
    evaluated_at      TIMESTAMP NULL,
    retry_count       INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_queue_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
        ON DELETE CASCADE
);


-- rules

CREATE TABLE rules (
    rule_id          INT AUTO_INCREMENT PRIMARY KEY,
    rule_name        VARCHAR(150) NOT NULL,
    rule_type        ENUM(
                        'AMOUNT_ANOMALY',
                        'VELOCITY',
                        'NEW_PAYEE',
                        'TIME_ANOMALY',
                        'DEVICE_CHANGE',
                        'LOCATION_CHANGE',
                        'SPENDING_PATTERN'
                    ) NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    weight           DECIMAL(4,3) NOT NULL DEFAULT 1.000,
    threshold_value  DECIMAL(15,2) NOT NULL,
    timeline         INT NOT NULL DEFAULT 30 COMMENT 'Lookback period in days, e.g. 10/20/30'
);


-- rule_evaluations


CREATE TABLE rule_evaluations (
    evaluation_id   INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  INT NOT NULL,
    rule_id         INT NOT NULL,
    risk_score      DECIMAL(4,3) NOT NULL,
    triggered       BOOLEAN NOT NULL DEFAULT FALSE,
    reason          VARCHAR(255),
    evaluated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_eval_rule
        FOREIGN KEY (rule_id) REFERENCES rules(rule_id)
        ON DELETE RESTRICT,
    INDEX idx_eval_transaction (transaction_id)
);



-- cases 

CREATE TABLE cases (
    case_id           INT AUTO_INCREMENT PRIMARY KEY,
    account_id        INT NOT NULL,
    risk_score        DECIMAL(5,2) NOT NULL,
    severity          ENUM('HIGH', 'MID', 'LOW') NOT NULL,
    status            ENUM('OPEN', 'IN_REVIEW', 'ESCALATED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at         TIMESTAMP NULL,
    resolution_notes  TEXT,
    CONSTRAINT fk_cases_account
        FOREIGN KEY (account_id) REFERENCES accounts(account_id)
        ON DELETE CASCADE,
    INDEX idx_cases_account (account_id)
);


-- alerts 

CREATE TABLE alerts (
    alert_id          INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id    INT NOT NULL,
    case_id           INT NULL,
    risk_score        DECIMAL(5,2) NOT NULL,
    severity          ENUM('HIGH', 'MID', 'LOW') NOT NULL,
    status            ENUM('OPEN', 'IN_REVIEW', 'ESCALATED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at         TIMESTAMP NULL,
    resolution_notes  TEXT,
    CONSTRAINT fk_alerts_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_alerts_case
        FOREIGN KEY (case_id) REFERENCES cases(case_id)
        ON DELETE SET NULL,
    INDEX idx_alerts_case (case_id)
);
