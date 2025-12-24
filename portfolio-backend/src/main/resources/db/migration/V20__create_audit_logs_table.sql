-- Migration V20: Create audit_logs table for comprehensive audit logging
-- Stores all auditable actions with JSONB for old/new value diffs

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,

    -- Action metadata
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    entity_name VARCHAR(255),

    -- User context
    user_id BIGINT,
    username VARCHAR(50) NOT NULL,
    user_role VARCHAR(20),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),

    -- Value changes (JSONB for flexible schema)
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],

    -- Request context
    request_method VARCHAR(10),
    request_uri VARCHAR(500),
    request_id VARCHAR(36),

    -- Result
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for common queries
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_username ON audit_logs(username);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_success ON audit_logs(success);

-- GIN index for JSONB queries
CREATE INDEX idx_audit_logs_old_values ON audit_logs USING GIN (old_values);
CREATE INDEX idx_audit_logs_new_values ON audit_logs USING GIN (new_values);

-- Comments
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all administrative actions';
COMMENT ON COLUMN audit_logs.action IS 'Action type: CREATE, UPDATE, DELETE, PUBLISH, LOGIN, etc.';
COMMENT ON COLUMN audit_logs.old_values IS 'Previous entity state as JSON (for UPDATE/DELETE)';
COMMENT ON COLUMN audit_logs.new_values IS 'New entity state as JSON (for CREATE/UPDATE)';
COMMENT ON COLUMN audit_logs.changed_fields IS 'Array of field names that were modified';
COMMENT ON COLUMN audit_logs.request_id IS 'UUID for correlating multiple audit entries from same request';
