-- =====================================================
-- Migration V10: Create hero_section table
-- =====================================================

-- Hero section table: singleton table for hero section content (id always = 1)
CREATE TABLE hero_section (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hero_section_single_row CHECK (id = 1)
);

-- Table comment for database documentation
COMMENT ON TABLE hero_section IS 'Singleton table containing hero section content for the home page';

-- Column comments
COMMENT ON COLUMN hero_section.title IS 'Hero section main title (e.g., Developpeur Backend)';
COMMENT ON COLUMN hero_section.description IS 'Hero section description text';
