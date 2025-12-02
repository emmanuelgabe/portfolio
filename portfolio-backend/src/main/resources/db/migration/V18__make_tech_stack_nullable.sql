-- Migration V18: Make tech_stack column nullable
-- Required for projects without details page (hasDetails = false)

ALTER TABLE projects
ALTER COLUMN tech_stack DROP NOT NULL;

COMMENT ON COLUMN projects.tech_stack IS 'Technologies used in the project. Required only when has_details is true.';
