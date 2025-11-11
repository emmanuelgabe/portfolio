-- =====================================================
-- Migration V1: Create projects and tags tables
-- =====================================================

-- Tags table: stores technology tags that can be associated with projects
CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(7) NOT NULL,
    CONSTRAINT chk_color_format CHECK (color ~ '^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$')
);

-- Index for tag name lookups
CREATE INDEX idx_tags_name ON tags(name);

-- Projects table: stores portfolio project information
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    tech_stack VARCHAR(500) NOT NULL,
    github_url VARCHAR(255),
    image_url VARCHAR(255),
    demo_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    featured BOOLEAN NOT NULL DEFAULT false
);

-- Index for filtering featured projects on homepage
CREATE INDEX idx_projects_featured ON projects(featured);

-- Index for project title searches
CREATE INDEX idx_projects_title ON projects(title);

-- Index for chronological sorting
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);

-- Junction table for many-to-many relationship between projects and tags
CREATE TABLE project_tags (
    project_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (project_id, tag_id),
    CONSTRAINT fk_project_tags_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Index for finding projects by tag
CREATE INDEX idx_project_tags_tag_id ON project_tags(tag_id);

-- Index for finding tags by project
CREATE INDEX idx_project_tags_project_id ON project_tags(project_id);

-- Table comments for database documentation
COMMENT ON TABLE tags IS 'Technology tags that can be associated with projects';
COMMENT ON TABLE projects IS 'Portfolio projects with details and links';
COMMENT ON TABLE project_tags IS 'Many-to-many relationship between projects and tags';

-- Column comments for important fields
COMMENT ON COLUMN tags.color IS 'Hex color code for tag display (e.g., #FF5733)';
COMMENT ON COLUMN projects.featured IS 'Flag indicating if project should be featured on homepage';
COMMENT ON COLUMN projects.tech_stack IS 'Comma-separated or JSON list of technologies used';