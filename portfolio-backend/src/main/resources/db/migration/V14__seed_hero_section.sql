-- =====================================================
-- Migration V14: Seed hero_section table
-- =====================================================

-- Insert default hero section row (singleton with id=1)
INSERT INTO hero_section (id, title, description, created_at, updated_at)
VALUES (
    1,
    'Developpeur Backend',
    'Passionne par le developpement logiciel et les architectures robustes.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
