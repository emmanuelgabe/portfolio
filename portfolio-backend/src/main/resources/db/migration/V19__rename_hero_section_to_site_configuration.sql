-- =====================================================
-- Migration V19: Rename hero_section to site_configuration
-- Adds site-wide configuration fields (name, email, social links, SEO)
-- =====================================================

-- Rename the table
ALTER TABLE hero_section RENAME TO site_configuration;

-- Rename existing columns for clarity
ALTER TABLE site_configuration RENAME COLUMN title TO hero_title;
ALTER TABLE site_configuration RENAME COLUMN description TO hero_description;

-- Add new configuration fields with defaults (for existing row)
ALTER TABLE site_configuration ADD COLUMN full_name VARCHAR(100) NOT NULL DEFAULT 'Emmanuel Gabe';
ALTER TABLE site_configuration ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT 'contact@emmanuelgabe.com';
ALTER TABLE site_configuration ADD COLUMN site_title VARCHAR(100) NOT NULL DEFAULT 'Portfolio - Emmanuel Gabe';
ALTER TABLE site_configuration ADD COLUMN seo_description VARCHAR(300) NOT NULL DEFAULT 'Portfolio de Emmanuel Gabe, Developpeur Backend specialise en Java et Spring Boot.';
ALTER TABLE site_configuration ADD COLUMN profile_image_path VARCHAR(500);
ALTER TABLE site_configuration ADD COLUMN github_url VARCHAR(500) NOT NULL DEFAULT 'https://github.com/emmanuelgabe';
ALTER TABLE site_configuration ADD COLUMN linkedin_url VARCHAR(500) NOT NULL DEFAULT 'https://linkedin.com/in/egabe';

-- Remove defaults after data migration (keep NOT NULL constraint)
ALTER TABLE site_configuration ALTER COLUMN full_name DROP DEFAULT;
ALTER TABLE site_configuration ALTER COLUMN email DROP DEFAULT;
ALTER TABLE site_configuration ALTER COLUMN site_title DROP DEFAULT;
ALTER TABLE site_configuration ALTER COLUMN seo_description DROP DEFAULT;
ALTER TABLE site_configuration ALTER COLUMN github_url DROP DEFAULT;
ALTER TABLE site_configuration ALTER COLUMN linkedin_url DROP DEFAULT;

-- Update constraint name
ALTER TABLE site_configuration DROP CONSTRAINT IF EXISTS chk_hero_section_single_row;
ALTER TABLE site_configuration ADD CONSTRAINT chk_site_configuration_single_row CHECK (id = 1);

-- Update table comment
COMMENT ON TABLE site_configuration IS 'Singleton table containing site-wide configuration (hero section, identity, SEO, social links)';

-- Column comments
COMMENT ON COLUMN site_configuration.full_name IS 'Site owner full name (e.g., Emmanuel Gabe)';
COMMENT ON COLUMN site_configuration.email IS 'Contact email address';
COMMENT ON COLUMN site_configuration.hero_title IS 'Hero section main title (e.g., Developpeur Backend)';
COMMENT ON COLUMN site_configuration.hero_description IS 'Hero section description text';
COMMENT ON COLUMN site_configuration.site_title IS 'Browser tab title and SEO title';
COMMENT ON COLUMN site_configuration.seo_description IS 'Meta description for SEO';
COMMENT ON COLUMN site_configuration.profile_image_path IS 'Path to profile image (nullable)';
COMMENT ON COLUMN site_configuration.github_url IS 'GitHub profile URL';
COMMENT ON COLUMN site_configuration.linkedin_url IS 'LinkedIn profile URL';
