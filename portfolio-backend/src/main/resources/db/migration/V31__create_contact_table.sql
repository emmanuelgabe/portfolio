CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(15),
    moderation_verdict VARCHAR(10) NOT NULL,
    moderation_reason TEXT,
    confidence_score DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contacts_category ON contacts(category);
CREATE INDEX idx_contacts_created_at ON contacts(created_at);
CREATE INDEX idx_contacts_moderation_verdict ON contacts(moderation_verdict);

COMMENT ON TABLE contacts IS 'Contact form submissions with AI moderation results. GDPR retention: 12 months.';
COMMENT ON COLUMN contacts.category IS 'AI-assigned category: MISSION, QUESTION, FEEDBACK, SPAM, INAPPROPRIATE';
COMMENT ON COLUMN contacts.moderation_verdict IS 'AI moderation decision: APPROVED or REJECTED';
COMMENT ON COLUMN contacts.confidence_score IS 'AI confidence in the moderation decision (0.0 to 1.0)';
