-- =====================================================
-- Migration V27: Add show_months to experiences table
-- =====================================================

ALTER TABLE experiences
ADD COLUMN show_months BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN experiences.show_months IS 'Whether to display months in the experience date range';
