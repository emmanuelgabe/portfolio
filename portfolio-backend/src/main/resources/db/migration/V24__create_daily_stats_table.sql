-- Daily statistics table for aggregated portfolio metrics
CREATE TABLE daily_stats (
    id BIGSERIAL PRIMARY KEY,
    stats_date DATE NOT NULL UNIQUE,
    total_projects BIGINT NOT NULL DEFAULT 0,
    total_articles BIGINT NOT NULL DEFAULT 0,
    published_articles BIGINT NOT NULL DEFAULT 0,
    draft_articles BIGINT NOT NULL DEFAULT 0,
    total_skills BIGINT NOT NULL DEFAULT 0,
    total_experiences BIGINT NOT NULL DEFAULT 0,
    total_tags BIGINT NOT NULL DEFAULT 0,
    total_project_images BIGINT NOT NULL DEFAULT 0,
    total_article_images BIGINT NOT NULL DEFAULT 0,
    contact_submissions BIGINT NOT NULL DEFAULT 0,
    audit_events_count BIGINT NOT NULL DEFAULT 0,
    failed_audit_events BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient date-based queries
CREATE INDEX idx_daily_stats_date ON daily_stats(stats_date DESC);

-- Comment on table
COMMENT ON TABLE daily_stats IS 'Aggregated daily statistics for portfolio metrics';
