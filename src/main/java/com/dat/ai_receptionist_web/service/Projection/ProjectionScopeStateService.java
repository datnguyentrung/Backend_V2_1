package com.dat.ai_receptionist_web.service.Projection;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectionScopeStateService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void beginRebuild(String scopeKey, String generation) {
        int updated = jdbcTemplate.update("""
                INSERT INTO operation.projection_scope_state (
                    scope_key, rebuilding, rebuild_started_at, rebuild_generation, updated_at
                ) VALUES (?, TRUE, NOW(), ?, NOW())
                ON CONFLICT (scope_key) DO UPDATE SET
                    rebuilding = TRUE,
                    rebuild_started_at = NOW(),
                    rebuild_generation = EXCLUDED.rebuild_generation,
                    updated_at = NOW()
                WHERE operation.projection_scope_state.rebuilding = FALSE
                   OR operation.projection_scope_state.rebuild_started_at < NOW() - INTERVAL '30 minutes'
                """, scopeKey, generation);
        if (updated != 1) {
            throw new IllegalStateException("Leaderboard rebuild already running for scope " + scopeKey);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void endRebuild(String scopeKey, String generation) {
        jdbcTemplate.update("""
                UPDATE operation.projection_scope_state
                SET rebuilding = FALSE, rebuild_generation = NULL, updated_at = NOW()
                WHERE scope_key = ? AND rebuild_generation = ?
                """, scopeKey, generation);
    }
}
