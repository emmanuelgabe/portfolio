-- Add status column to project_images table for async processing tracking
ALTER TABLE project_images
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY';

-- Set existing images to READY since they were processed synchronously
-- New images will default to PROCESSING and be updated by the consumer

-- Add index for status queries
CREATE INDEX idx_project_images_status ON project_images(status);
