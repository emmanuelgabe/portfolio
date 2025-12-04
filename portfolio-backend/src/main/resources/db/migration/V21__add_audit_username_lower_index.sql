-- Migration V21: Add expression index for case-insensitive username search
-- Optimizes queries using LOWER(username) LIKE '%...%' pattern

CREATE INDEX idx_audit_logs_username_lower ON audit_logs(LOWER(username));

COMMENT ON INDEX idx_audit_logs_username_lower IS 'Expression index for case-insensitive username searches';
