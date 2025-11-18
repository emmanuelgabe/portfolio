-- V4: Remove level column from skills table
ALTER TABLE skills DROP COLUMN IF EXISTS level;
