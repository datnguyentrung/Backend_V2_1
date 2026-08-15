CREATE TABLE operation.projection_outbox (
    id BIGSERIAL PRIMARY KEY,
    projection_type VARCHAR(64) NOT NULL,
    projection_key VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_key VARCHAR(128) NOT NULL,
    year_value INTEGER,
    quarter_value INTEGER,
    skill_level VARCHAR(32),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    revision BIGINT NOT NULL DEFAULT 1,
    processed_revision BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    locked_by VARCHAR(128),
    locked_at TIMESTAMPTZ,
    last_error TEXT,
    last_error_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    dirty_since TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT uq_projection_outbox_key UNIQUE (projection_key)
);

CREATE INDEX idx_projection_outbox_ready
    ON operation.projection_outbox (status, next_attempt_at);

CREATE INDEX idx_projection_outbox_scope
    ON operation.projection_outbox (projection_type, year_value, quarter_value, skill_level);