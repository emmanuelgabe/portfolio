-- =====================================================
-- Migration V7: Create experiences table
-- =====================================================

-- Experiences table: stores professional, educational, certification, and volunteering experiences
CREATE TABLE experiences (
    id BIGSERIAL PRIMARY KEY,
    company VARCHAR(200) NOT NULL,
    role VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    description TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_experience_type CHECK (type IN ('WORK', 'EDUCATION', 'CERTIFICATION', 'VOLUNTEERING')),
    CONSTRAINT chk_end_date_after_start CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Index for chronological sorting (most recent first)
CREATE INDEX idx_experiences_start_date ON experiences(start_date DESC);

-- Index for filtering by experience type
CREATE INDEX idx_experiences_type ON experiences(type);

-- Index for finding ongoing experiences (where end_date is NULL)
CREATE INDEX idx_experiences_ongoing ON experiences(end_date) WHERE end_date IS NULL;

-- Table comments for database documentation
COMMENT ON TABLE experiences IS 'Professional, educational, certification, and volunteering experiences for timeline display';

-- Column comments for important fields
COMMENT ON COLUMN experiences.company IS 'Company name or educational institution';
COMMENT ON COLUMN experiences.role IS 'Job title, degree name, or certification name';
COMMENT ON COLUMN experiences.start_date IS 'Start date of the experience';
COMMENT ON COLUMN experiences.end_date IS 'End date of the experience (NULL if ongoing)';
COMMENT ON COLUMN experiences.type IS 'Type of experience: WORK, EDUCATION, CERTIFICATION, or VOLUNTEERING';
COMMENT ON COLUMN experiences.description IS 'Detailed description of the experience, achievements, and responsibilities';
