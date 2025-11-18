-- =====================================================
-- Migration V3: Create users and refresh tokens tables
-- =====================================================

-- Users table: stores authenticated users with roles and credentials
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_GUEST',
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('ROLE_ADMIN', 'ROLE_GUEST')),
    CONSTRAINT chk_username_length CHECK (char_length(username) >= 3),
    CONSTRAINT chk_email_format CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Refresh tokens table: stores JWT refresh tokens for token rotation
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for users table
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_enabled ON users(enabled);

-- Indexes for refresh_tokens table
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);

-- Table comments for database documentation
COMMENT ON TABLE users IS 'Authenticated users with role-based access control';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for secure token rotation and revocation';

-- Column comments for users table
COMMENT ON COLUMN users.username IS 'Unique username for authentication (3-50 characters)';
COMMENT ON COLUMN users.email IS 'Unique email address in valid format';
COMMENT ON COLUMN users.password IS 'BCrypt hashed password';
COMMENT ON COLUMN users.role IS 'User role: ROLE_ADMIN (full access) or ROLE_GUEST (read-only)';
COMMENT ON COLUMN users.enabled IS 'Account activation status';
COMMENT ON COLUMN users.account_non_expired IS 'Account expiration status for Spring Security';
COMMENT ON COLUMN users.account_non_locked IS 'Account lock status for Spring Security';
COMMENT ON COLUMN users.credentials_non_expired IS 'Password expiration status for Spring Security';

-- Column comments for refresh_tokens table
COMMENT ON COLUMN refresh_tokens.token IS 'UUID refresh token string';
COMMENT ON COLUMN refresh_tokens.user_id IS 'Reference to user owning this token';
COMMENT ON COLUMN refresh_tokens.expiry_date IS 'Token expiration timestamp';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Token revocation flag for logout and security';

-- =====================================================
-- Seed default admin user
-- =====================================================
-- Default admin credentials:
-- Username: admin
-- Password: Admin123! (BCrypt hash below)
-- IMPORTANT: Change this password immediately in production via /api/auth/change-password!

INSERT INTO users (username, email, password, role, enabled)
VALUES (
    'admin',
    'admin@portfolio.local',
    '$2a$10$D5vUvUFfFibOmDxPiAp1sesriAY16yfFI2uIjiyT4wAZa8HeYOG6u',
    'ROLE_ADMIN',
    true
);

-- Note: The BCrypt hash above is for password: Admin123!
-- This is a well-known test hash - DO NOT use in production!
-- Change password immediately after first login using the /api/auth/change-password endpoint
