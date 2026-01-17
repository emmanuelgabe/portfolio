-- =====================================================
-- Migration V29: Create project_details table for Markdown content
-- =====================================================

-- Create project_details table
CREATE TABLE project_details (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_details_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Add index on project_id for fast lookups
CREATE INDEX idx_project_details_project_id ON project_details(project_id);

-- Add comments
COMMENT ON TABLE project_details IS 'Stores detailed Markdown content for projects (unlimited length)';
COMMENT ON COLUMN project_details.content IS 'Markdown content with rich formatting (unlimited)';
COMMENT ON COLUMN project_details.project_id IS 'Foreign key to projects table (one-to-one relation)';

-- Migrate existing project descriptions to project_details
-- Only for projects where hasDetails = true AND description is not empty
INSERT INTO project_details (project_id, content, created_at, updated_at)
SELECT
    id,
    description,
    created_at,
    updated_at
FROM projects
WHERE has_details = true
  AND description IS NOT NULL
  AND TRIM(description) <> '';

-- Keep short description in projects table for list views (no changes to projects table structure)
-- The 'description' column in projects table will remain as a summary/excerpt
-- The 'content' column in project_details will contain the full Markdown content
