-- =====================================================
-- Migration V16: Fix accents in hero_section table
-- =====================================================

-- Update hero section with correct French accents
UPDATE hero_section
SET title = 'Développeur Backend',
    description = REPLACE(
        REPLACE(description, 'Passionne', 'Passionné'),
        'developpement', 'développement'
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;
