CREATE TABLE operation.projection_scope_state (
    scope_key VARCHAR(255) PRIMARY KEY,
    rebuilding BOOLEAN NOT NULL DEFAULT FALSE,
    rebuild_started_at TIMESTAMPTZ,
    rebuild_generation VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projection_scope_state_rebuilding
    ON operation.projection_scope_state (rebuilding)
    WHERE rebuilding = TRUE;
