-- AI Health Monitor: diagnostic reports and proposed actions
-- Task 3.1 -- supports the tiered agent (green observe, orange/red propose)

CREATE TABLE health_reports (
    id               BIGSERIAL PRIMARY KEY,
    cycle_type       VARCHAR(20)  NOT NULL,
    severity         VARCHAR(15)  NOT NULL,
    summary          TEXT         NOT NULL,
    anomalies_json   TEXT,
    tool_calls_json  TEXT,
    duration_ms      BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL
);

CREATE INDEX idx_health_reports_created_at ON health_reports (created_at DESC);
CREATE INDEX idx_health_reports_severity   ON health_reports (severity);

CREATE TABLE health_proposed_actions (
    id                BIGSERIAL PRIMARY KEY,
    report_id         BIGINT       NOT NULL REFERENCES health_reports(id) ON DELETE CASCADE,
    level             VARCHAR(10)  NOT NULL,
    action_type       VARCHAR(40)  NOT NULL,
    params_json       TEXT,
    justification     TEXT         NOT NULL,
    status            VARCHAR(15)  NOT NULL,
    approved_by       VARCHAR(100),
    approved_at       TIMESTAMP,
    executed_at       TIMESTAMP,
    rejection_reason  TEXT,
    result_json       TEXT,
    expires_at        TIMESTAMP    NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_health_proposed_actions_status     ON health_proposed_actions (status);
CREATE INDEX idx_health_proposed_actions_report_id  ON health_proposed_actions (report_id);
CREATE INDEX idx_health_proposed_actions_expires_at ON health_proposed_actions (expires_at);
CREATE INDEX idx_health_proposed_actions_created_at ON health_proposed_actions (created_at DESC);
