-- =====================================================
-- Migration V11: Add icon type support for skills
-- Enables Font Awesome icons and custom SVG uploads
-- =====================================================

-- Add icon_type column to distinguish between FA icons and custom SVG
ALTER TABLE skills ADD COLUMN icon_type VARCHAR(20) NOT NULL DEFAULT 'FONT_AWESOME';

-- Add custom_icon_url for SVG uploads (nullable)
ALTER TABLE skills ADD COLUMN custom_icon_url VARCHAR(500);

-- Add constraint for icon_type values
ALTER TABLE skills ADD CONSTRAINT chk_icon_type
    CHECK (icon_type IN ('FONT_AWESOME', 'CUSTOM_SVG'));

-- Migrate existing Bootstrap Icons to Font Awesome equivalents
UPDATE skills SET icon =
    CASE icon
        WHEN 'bi-code-slash' THEN 'fa-solid fa-code'
        WHEN 'bi-code' THEN 'fa-solid fa-code'
        WHEN 'bi-gear' THEN 'fa-solid fa-gear'
        WHEN 'bi-gear-fill' THEN 'fa-solid fa-gear'
        WHEN 'bi-database' THEN 'fa-solid fa-database'
        WHEN 'bi-database-fill' THEN 'fa-solid fa-database'
        WHEN 'bi-cloud' THEN 'fa-solid fa-cloud'
        WHEN 'bi-cloud-fill' THEN 'fa-solid fa-cloud'
        WHEN 'bi-server' THEN 'fa-solid fa-server'
        WHEN 'bi-terminal' THEN 'fa-solid fa-terminal'
        WHEN 'bi-terminal-fill' THEN 'fa-solid fa-terminal'
        WHEN 'bi-braces' THEN 'fa-solid fa-code'
        WHEN 'bi-braces-asterisk' THEN 'fa-solid fa-code'
        WHEN 'bi-cpu' THEN 'fa-solid fa-microchip'
        WHEN 'bi-cpu-fill' THEN 'fa-solid fa-microchip'
        WHEN 'bi-diagram-3' THEN 'fa-solid fa-diagram-project'
        WHEN 'bi-diagram-3-fill' THEN 'fa-solid fa-diagram-project'
        WHEN 'bi-filetype-html' THEN 'fa-brands fa-html5'
        WHEN 'bi-filetype-css' THEN 'fa-brands fa-css3-alt'
        WHEN 'bi-filetype-js' THEN 'fa-brands fa-js'
        WHEN 'bi-filetype-java' THEN 'fa-brands fa-java'
        WHEN 'bi-filetype-py' THEN 'fa-brands fa-python'
        WHEN 'bi-git' THEN 'fa-brands fa-git-alt'
        WHEN 'bi-github' THEN 'fa-brands fa-github'
        WHEN 'bi-bootstrap' THEN 'fa-brands fa-bootstrap'
        WHEN 'bi-windows' THEN 'fa-brands fa-windows'
        WHEN 'bi-apple' THEN 'fa-brands fa-apple'
        WHEN 'bi-linux' THEN 'fa-brands fa-linux'
        WHEN 'bi-android' THEN 'fa-brands fa-android'
        ELSE CASE
            WHEN icon LIKE 'bi-%' THEN 'fa-solid fa-code'
            ELSE icon
        END
    END
WHERE icon LIKE 'bi-%';

-- Update column comments
COMMENT ON COLUMN skills.icon IS 'Font Awesome icon class (e.g., fa-solid fa-code, fa-brands fa-angular)';
COMMENT ON COLUMN skills.icon_type IS 'Icon source type: FONT_AWESOME or CUSTOM_SVG';
COMMENT ON COLUMN skills.custom_icon_url IS 'URL to custom uploaded SVG icon (only when icon_type is CUSTOM_SVG)';
