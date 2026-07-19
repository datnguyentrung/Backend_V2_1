package com.dat.backend_v2_1.migration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceClassSessionMigrationSqlTest {

    @Test
    void historicalRowsWithNoMatchingClassSessionAreReported() throws Exception {
        String sql = resource("/db/migration/V4__report_attendance_class_session_unresolved.sql");

        assertTrue(sql.contains("NO_MATCHING_CLASS_SESSION"));
        assertTrue(sql.contains("matching_session_count = 0"));
    }

    @Test
    void historicalRowsWithMultipleMatchingClassSessionsAreReported() throws Exception {
        String sql = resource("/db/migration/V4__report_attendance_class_session_unresolved.sql");

        assertTrue(sql.contains("MULTIPLE_MATCHING_CLASS_SESSIONS"));
        assertTrue(sql.contains("matching_session_count <> 1"));
    }

    @Test
    void enforceMigrationFailsBeforeSettingNotNullWhenUnresolvedRowsExist() throws Exception {
        String sql = resource("/db/migration/V5__enforce_attendance_class_session_constraints.sql");

        int raiseExceptionIndex = sql.indexOf("RAISE EXCEPTION");
        int setNotNullIndex = sql.indexOf("ALTER COLUMN class_session_id SET NOT NULL");

        assertTrue(raiseExceptionIndex >= 0);
        assertTrue(setNotNullIndex > raiseExceptionIndex);
    }

    private String resource(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
