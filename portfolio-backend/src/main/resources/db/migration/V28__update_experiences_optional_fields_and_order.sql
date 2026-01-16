-- =====================================================
-- Migration V28: Update experiences optional fields and ordering
-- =====================================================

ALTER TABLE experiences
ADD COLUMN display_order INTEGER;

ALTER TABLE experiences
    ALTER COLUMN company DROP NOT NULL,
    ALTER COLUMN role DROP NOT NULL,
    ALTER COLUMN start_date DROP NOT NULL,
    ALTER COLUMN type DROP NOT NULL;

ALTER TABLE experiences
    DROP CONSTRAINT IF EXISTS chk_experience_type;

ALTER TABLE experiences
    ADD CONSTRAINT chk_experience_type
        CHECK (type IN ('WORK', 'STAGE', 'EDUCATION', 'CERTIFICATION', 'VOLUNTEERING'));

COMMENT ON COLUMN experiences.display_order IS 'Manual display order for experiences';
