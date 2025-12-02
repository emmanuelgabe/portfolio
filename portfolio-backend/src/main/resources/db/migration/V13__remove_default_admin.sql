-- =====================================================
-- Migration V13: Remove default admin user
-- =====================================================
-- Security fix: Remove hardcoded admin user from V3 migration.
-- Admin user will now be created by AdminSeeder using
-- password hash from environment variable.

DELETE FROM users WHERE username = 'admin';
