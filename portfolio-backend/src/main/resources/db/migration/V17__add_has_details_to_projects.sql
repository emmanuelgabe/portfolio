-- Migration V17: Add has_details column to projects table
-- Allows projects to be displayed without a detail page link

-- Add has_details column with default value true (existing projects have details)
ALTER TABLE projects
ADD COLUMN has_details BOOLEAN NOT NULL DEFAULT true;

-- Add comment for documentation
COMMENT ON COLUMN projects.has_details IS 'Whether the project has a detail page. If false, the card is not clickable and tech stack is hidden.';
