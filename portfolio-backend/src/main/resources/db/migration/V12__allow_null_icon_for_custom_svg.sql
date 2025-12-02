-- =====================================================
-- Migration V12: Allow null icon for custom SVG skills
-- When using CUSTOM_SVG icon type, the icon column is not needed
-- =====================================================

-- Remove NOT NULL constraint from icon column
ALTER TABLE skills ALTER COLUMN icon DROP NOT NULL;

-- Add constraint: icon must be set when using FONT_AWESOME, or custom_icon_url must be set when using CUSTOM_SVG
ALTER TABLE skills ADD CONSTRAINT chk_icon_or_custom_svg
    CHECK (
        (icon_type = 'FONT_AWESOME' AND icon IS NOT NULL)
        OR (icon_type = 'CUSTOM_SVG' AND custom_icon_url IS NOT NULL)
    );

-- Update column comment
COMMENT ON COLUMN skills.icon IS 'Font Awesome icon class (required when icon_type is FONT_AWESOME, null when CUSTOM_SVG)';
