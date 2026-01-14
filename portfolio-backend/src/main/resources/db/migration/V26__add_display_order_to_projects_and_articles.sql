-- =====================================================
-- Migration V26: Add display_order to projects and articles tables
-- =====================================================

-- Add display_order column to projects table
ALTER TABLE projects
ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

-- Add display_order column to articles table
ALTER TABLE articles
ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

-- Add check constraint for projects display_order
ALTER TABLE projects
ADD CONSTRAINT chk_projects_display_order CHECK (display_order >= 0);

-- Add check constraint for articles display_order
ALTER TABLE articles
ADD CONSTRAINT chk_articles_display_order CHECK (display_order >= 0);

-- Create indexes for ordering
CREATE INDEX idx_projects_display_order ON projects(display_order);
CREATE INDEX idx_articles_display_order ON articles(display_order);

-- Initialize display_order based on creation date (oldest first)
WITH ranked_projects AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at ASC) - 1 as new_order
    FROM projects
)
UPDATE projects p
SET display_order = rp.new_order
FROM ranked_projects rp
WHERE p.id = rp.id;

WITH ranked_articles AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at ASC) - 1 as new_order
    FROM articles
)
UPDATE articles a
SET display_order = ra.new_order
FROM ranked_articles ra
WHERE a.id = ra.id;

-- Column comments
COMMENT ON COLUMN projects.display_order IS 'Order in which projects should be displayed (lower numbers first)';
COMMENT ON COLUMN articles.display_order IS 'Order in which articles should be displayed (lower numbers first)';
