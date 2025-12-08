-- Add unique_visitors column to daily_stats table
ALTER TABLE daily_stats ADD COLUMN unique_visitors BIGINT NOT NULL DEFAULT 0;
