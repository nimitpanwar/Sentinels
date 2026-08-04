
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
                        'AMOUNT_THRESHOLD',
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
-- NOTE: status/severity values and extra columns below match the CURRENT
-- code (entity.Case / enums.CaseStatus / enums.ResolutionReasonCode) as of
-- 2026-08-04 - this replaced an earlier OPEN/IN_REVIEW/ESCALATED/CLOSED
-- design. See AlertManager.java for the enforced state-machine transitions.

CREATE TABLE cases (
    case_id                 INT AUTO_INCREMENT PRIMARY KEY,
    account_id              INT NOT NULL,
    version                 BIGINT NOT NULL DEFAULT 0 COMMENT 'JPA @Version optimistic lock - guards concurrent acknowledge/investigate/close/dismiss',
    risk_score              DECIMAL(5,2) NOT NULL,
    severity                ENUM('HIGH', 'MID', 'LOW') NOT NULL,
    status                  ENUM('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED') NOT NULL DEFAULT 'OPEN',
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at               TIMESTAMP NULL,
    acknowledged_at         TIMESTAMP NULL COMMENT 'Set the first time this case is acknowledged - feeds the avg-time-to-acknowledge stat',
    resolution_notes        TEXT,
    resolution_reason_code  ENUM(
                                'CONFIRMED_FRAUD',
                                'FALSE_POSITIVE_KNOWN_CUSTOMER',
                                'FALSE_POSITIVE_RULE_TOO_SENSITIVE',
                                'LEGITIMATE_LARGE_PURCHASE',
                                'DUPLICATE_ALERT',
                                'INSUFFICIENT_EVIDENCE'
                            ) NULL COMMENT 'Set only when status becomes CLOSED/DISMISSED - CONFIRMED_FRAUD seeds the network-analysis job''s personalized PageRank',
    last_alert_at           TIMESTAMP NULL COMMENT 'Timestamp of the most recent alert merged into this case - drives the merge-cooldown window check',
    CONSTRAINT fk_cases_account
        FOREIGN KEY (account_id) REFERENCES accounts(account_id)
        ON DELETE CASCADE,
    INDEX idx_cases_account (account_id),
    INDEX idx_case_account_status (account_id, status)
);


-- alerts
-- NOTE: status now mirrors cases.status (5-value CaseStatus enum) instead of
-- the old OPEN/IN_REVIEW/ESCALATED/CLOSED set - kept in sync by AlertManager
-- on every case lifecycle change.

CREATE TABLE alerts (
    alert_id          INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id    INT NOT NULL,
    case_id           INT NULL,
    risk_score        DECIMAL(5,2) NOT NULL,
    severity          ENUM('HIGH', 'MID', 'LOW') NOT NULL,
    status            ENUM('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED') NOT NULL DEFAULT 'OPEN',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at         TIMESTAMP NULL,
    resolution_notes  TEXT,
    CONSTRAINT fk_alerts_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_alerts_case
        FOREIGN KEY (case_id) REFERENCES cases(case_id)
        ON DELETE SET NULL,
    INDEX idx_alerts_case (case_id),
    INDEX idx_alert_case_created (case_id, created_at),
    INDEX idx_alert_transaction (transaction_id)
);


-- alert_settings
-- Single-row (id always 1) global config table - replaces the old hardcoded
-- AlertConfig. Editable at runtime via PUT /api/alert-settings. The app
-- auto-seeds this row with the defaults below the first time it's read if
-- it doesn't exist yet (see riskengine.config.AlertConfig.seedDefaults), so
-- an explicit INSERT here is optional but included for a ready-to-go DB.

CREATE TABLE alert_settings (
    id                        INT PRIMARY KEY,
    min_score_to_create_alert INT NOT NULL DEFAULT 50 COMMENT 'Risk scores below this never create an alert',
    low_severity_max          INT NOT NULL DEFAULT 60 COMMENT 'Scores <= this are LOW severity',
    medium_severity_max       INT NOT NULL DEFAULT 80 COMMENT 'Scores <= this are MID, above is HIGH',
    merge_cooldown_minutes    INT NOT NULL DEFAULT 60 COMMENT 'Window to merge a new alert into an existing open case for the same account'
);

INSERT INTO alert_settings (id, min_score_to_create_alert, low_severity_max, medium_severity_max, merge_cooldown_minutes)
VALUES (1, 50, 60, 80, 60);


-- ═══════════════════════════════════════════════════════════════════════
-- NETWORK ANALYSIS TABLES
-- Written/maintained by the separate Python (network-analysis/) batch job,
-- read-only from the Spring side (NetworkController). Hibernate's
-- ddl-auto=update would auto-create these on app startup anyway, but they
-- are defined explicitly here so the DB works standalone (e.g. running the
-- Python job before ever starting the Spring app, or setting up a fresh DB
-- without running Spring first).
-- ═══════════════════════════════════════════════════════════════════════

-- network_runs
-- One row per execution of the network-analysis job. Written by Python via
-- start_run()/complete_run()/fail_run() - RUNNING -> COMPLETED/FAILED.

CREATE TABLE network_runs (
    run_id             INT AUTO_INCREMENT PRIMARY KEY,
    started_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at       TIMESTAMP NULL,
    status             ENUM('RUNNING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'RUNNING',
    trigger_type       ENUM('SCHEDULED', 'MANUAL') NOT NULL DEFAULT 'SCHEDULED',
    lookback_days      INT NOT NULL,
    algorithm_version  VARCHAR(20) NOT NULL COMMENT 'e.g. "1.0.0" - bumped whenever the scoring formula/weights change',
    accounts_analyzed  INT NULL,
    accounts_flagged   INT NULL COMMENT 'Count of accounts whose network_risk_score >= FLAGGED_SCORE_THRESHOLD (config.py), for the operator summary card',
    error_message      TEXT NULL COMMENT 'Full Python traceback on FAILED runs'
);


-- account_network_scores
-- One row per account PER RUN (append-only history - never overwritten),
-- so an account's network risk can be tracked over time.

CREATE TABLE account_network_scores (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id                INT NOT NULL,
    account_id            INT NOT NULL,
    network_risk_score    DECIMAL(5,2) NOT NULL COMMENT 'Final weighted 0-100 score combining all signals below',
    page_rank_percentile  DECIMAL(5,2) NULL COMMENT 'Personalized-PageRank fraud-exposure signal, as a 0-100 percentile rank',
    shared_payee_count    INT NULL,
    community_id          INT NULL COMMENT 'Louvain community id this account belongs to',
    community_size        INT NULL,
    growth_score          DECIMAL(5,2) NULL COMMENT 'Relationship-growth signal (new payees acquired recently), 0-100 percentile',
    fraud_exposure_score  DECIMAL(5,2) NULL,
    evidence_json         JSON NULL COMMENT 'Raw per-signal values, e.g. {"page_rank":0.81,"shared_payees":17,...} - full explainability without re-running Python',
    network_reason        TEXT NULL COMMENT 'Deterministic, human-readable explanation generated by scoring.py at compute time',
    computed_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_netscore_run
        FOREIGN KEY (run_id) REFERENCES network_runs(run_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_netscore_account
        FOREIGN KEY (account_id) REFERENCES accounts(account_id)
        ON DELETE CASCADE,
    INDEX idx_net_score_account_computed (account_id, computed_at),
    INDEX idx_net_score_run (run_id)
);


-- network_run_requests
-- DB-mediated queue for SCHEDULED (non-manual) "run analysis" requests -
-- see scheduler.py. The manual "Run Analysis Now" UI button no longer uses
-- this table (it launches the Python subprocess directly and waits), but
-- scheduler.py's own periodic polling loop still can.

CREATE TABLE network_run_requests (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    requested_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lookback_days  INT NOT NULL DEFAULT 30,
    status         ENUM('PENDING', 'PICKED_UP', 'DONE') NOT NULL DEFAULT 'PENDING',
    picked_up_at   TIMESTAMP NULL
);
