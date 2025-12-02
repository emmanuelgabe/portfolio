-- Remove views_count column from articles table
ALTER TABLE articles DROP COLUMN IF EXISTS views_count;
