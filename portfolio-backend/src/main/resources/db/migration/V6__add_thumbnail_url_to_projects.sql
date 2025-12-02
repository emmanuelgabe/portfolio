-- Migration V6: Add thumbnail_url column to projects table
-- Adds support for storing optimized thumbnail images separately from main images

-- Add thumbnail_url column
ALTER TABLE projects
ADD COLUMN thumbnail_url VARCHAR(255);

-- Add comment for documentation
COMMENT ON COLUMN projects.thumbnail_url IS 'URL to the optimized thumbnail image (300x300px WebP)';

-- Optional: Create index if we plan to query by thumbnail_url (usually not needed)
-- CREATE INDEX idx_projects_thumbnail_url ON projects(thumbnail_url);
