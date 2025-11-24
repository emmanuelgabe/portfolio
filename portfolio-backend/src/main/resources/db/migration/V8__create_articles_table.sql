-- V8: Create articles table and related tables for blog functionality
-- Author: Emmanuel Gabe
-- Date: 2025-01-20

-- Table: articles
-- Description: Stores blog articles with Markdown content
CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    excerpt VARCHAR(500),
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    draft BOOLEAN NOT NULL DEFAULT true,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    views_count INT NOT NULL DEFAULT 0,
    reading_time_minutes INT
);

-- Table: article_tags (ManyToMany join table)
-- Description: Associates articles with tags
CREATE TABLE article_tags (
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_id)
);

-- Table: article_images
-- Description: Stores images associated with articles
CREATE TABLE article_images (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    image_url VARCHAR(255) NOT NULL,
    thumbnail_url VARCHAR(255),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance optimization
CREATE INDEX idx_articles_slug ON articles(slug);
CREATE INDEX idx_articles_published ON articles(published_at DESC) WHERE draft = false;
CREATE INDEX idx_articles_draft ON articles(draft);
CREATE INDEX idx_articles_author ON articles(author_id);
CREATE INDEX idx_article_tags_article ON article_tags(article_id);
CREATE INDEX idx_article_tags_tag ON article_tags(tag_id);
CREATE INDEX idx_article_images_article ON article_images(article_id);

-- Comments for documentation
COMMENT ON TABLE articles IS 'Blog articles with Markdown content and metadata';
COMMENT ON COLUMN articles.slug IS 'URL-friendly unique identifier generated from title';
COMMENT ON COLUMN articles.content IS 'Markdown source content';
COMMENT ON COLUMN articles.excerpt IS 'Short description for article cards and SEO';
COMMENT ON COLUMN articles.draft IS 'If true, article is not visible publicly';
COMMENT ON COLUMN articles.published_at IS 'Timestamp when article was published (null if draft)';
COMMENT ON COLUMN articles.views_count IS 'Number of times article has been viewed';
COMMENT ON COLUMN articles.reading_time_minutes IS 'Estimated reading time in minutes';

COMMENT ON TABLE article_tags IS 'Many-to-many relationship between articles and tags';
COMMENT ON TABLE article_images IS 'Images uploaded and embedded in articles';
