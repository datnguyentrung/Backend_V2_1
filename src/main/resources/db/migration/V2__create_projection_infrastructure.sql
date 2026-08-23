CREATE TABLE infrastructure.projection_outbox (
    id BIGSERIAL PRIMARY KEY,
    projection_type VARCHAR(64) NOT NULL,
    projection_key VARCHAR(255) NOT NULL UNIQUE,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_key VARCHAR(128) NOT NULL,
    year_value INTEGER,
    quarter_value INTEGER,
    schedule_level VARCHAR(32),
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
    CONSTRAINT ck_projection_schedule_level CHECK (
        schedule_level IS NULL OR schedule_level IN ('BASIC', 'ADVANCED', 'EXPERT')
    )
);

CREATE INDEX idx_projection_outbox_ready
    ON infrastructure.projection_outbox(status, next_attempt_at);

CREATE TABLE infrastructure.projection_scope_state (
    scope_key VARCHAR(255) PRIMARY KEY,
    rebuilding BOOLEAN NOT NULL DEFAULT FALSE,
    rebuild_started_at TIMESTAMPTZ,
    rebuild_generation VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
