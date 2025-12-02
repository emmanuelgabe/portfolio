-- Create CVs table for version-controlled CV uploads
CREATE TABLE cvs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_cv_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for faster queries by user
CREATE INDEX idx_cvs_user_id ON cvs(user_id);

-- Index for finding current CV
CREATE INDEX idx_cvs_current ON cvs(current);

-- Partial index for current CV per user (PostgreSQL specific)
CREATE UNIQUE INDEX idx_cvs_user_current ON cvs(user_id) WHERE current = true;
