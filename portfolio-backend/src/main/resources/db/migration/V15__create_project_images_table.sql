-- V15: Create project_images table for multi-image project support
-- Migrates existing single image fields to new table structure

-- Table: project_images
-- Description: Stores multiple images associated with portfolio projects
CREATE TABLE project_images (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    image_url VARCHAR(255) NOT NULL,
    thumbnail_url VARCHAR(255),
    alt_text VARCHAR(255),
    caption VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance optimization
CREATE INDEX idx_project_images_project ON project_images(project_id);
CREATE INDEX idx_project_images_primary ON project_images(project_id) WHERE is_primary = true;
CREATE INDEX idx_project_images_order ON project_images(project_id, display_order);

-- Comments for documentation
COMMENT ON TABLE project_images IS 'Images associated with portfolio projects, supports carousel display';
COMMENT ON COLUMN project_images.is_primary IS 'If true, this image is used as the project thumbnail in listings';
COMMENT ON COLUMN project_images.display_order IS 'Order for carousel display, 0-indexed';
COMMENT ON COLUMN project_images.alt_text IS 'Alt text for accessibility';
COMMENT ON COLUMN project_images.caption IS 'Optional caption displayed below image in carousel';

-- Migrate existing project images to new table
-- Existing single images become the primary image with display_order 0
INSERT INTO project_images (project_id, image_url, thumbnail_url, is_primary, display_order, uploaded_at)
SELECT id, image_url, thumbnail_url, true, 0, created_at
FROM projects
WHERE image_url IS NOT NULL AND image_url != '';
