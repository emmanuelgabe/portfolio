-- V30: Cleanup project_details table (not used) and change description to TEXT for unlimited Markdown
-- Author: Claude
-- Date: 2026-01-17

-- Drop project_details table (created in V29 but not used in final implementation)
DROP TABLE IF EXISTS project_details CASCADE;

-- Change description column from VARCHAR(2000) to TEXT for unlimited Markdown content
ALTER TABLE projects ALTER COLUMN description TYPE TEXT;

-- Add comment
COMMENT ON COLUMN projects.description IS 'Markdown-formatted project description (unlimited length)';
