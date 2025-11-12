-- =====================================================
-- Migration V2: Create skills table
-- =====================================================

-- Skills table: stores technical skills with categories and proficiency levels
CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(50) NOT NULL,
    color VARCHAR(7) NOT NULL,
    category VARCHAR(20) NOT NULL,
    level INTEGER NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_color_format CHECK (color ~ '^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$'),
    CONSTRAINT chk_level_range CHECK (level >= 0 AND level <= 100),
    CONSTRAINT chk_display_order CHECK (display_order >= 0),
    CONSTRAINT chk_category CHECK (category IN ('FRONTEND', 'BACKEND', 'DATABASE', 'DEVOPS', 'TOOLS'))
);

-- Index for ordering skills by display order
CREATE INDEX idx_skills_display_order ON skills(display_order);

-- Index for filtering skills by category
CREATE INDEX idx_skills_category ON skills(category);

-- Index for skill name searches
CREATE INDEX idx_skills_name ON skills(name);

-- Table comment for database documentation
COMMENT ON TABLE skills IS 'Technical skills with categories, proficiency levels, and visual attributes';

-- Column comments for important fields
COMMENT ON COLUMN skills.icon IS 'Bootstrap Icons class name for skill icon (e.g., bi-code-square)';
COMMENT ON COLUMN skills.color IS 'Hex color code for skill display (e.g., #dd0031)';
COMMENT ON COLUMN skills.category IS 'Skill category: FRONTEND, BACKEND, DATABASE, DEVOPS, or TOOLS';
COMMENT ON COLUMN skills.level IS 'Proficiency level from 0 to 100';
COMMENT ON COLUMN skills.display_order IS 'Order in which skills should be displayed (lower numbers first)';
