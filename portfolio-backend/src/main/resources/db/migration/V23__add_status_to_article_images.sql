-- Add status column to article_images table for async processing tracking
ALTER TABLE article_images
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY';

-- Create index for filtering by status
CREATE INDEX idx_article_images_status ON article_images(status);

-- Comment: Existing images are set to READY since they were processed synchronously
