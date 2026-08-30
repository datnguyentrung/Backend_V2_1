package com.dat.ai_receptionist_web.schema;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationContractTest {
    @Test
    void finalBootstrapContainsExactFinancialEnumsAndNoTuitionLegacy() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V1__create_final_domain_schema.sql"));

        assertThat(sql).contains("CREATE TABLE finance.wallet (")
                .contains("UNIQUE REFERENCES core.person(person_id)")
                .contains("'TOP_UP', 'COURSE_PURCHASE', 'SUPPLY_PURCHASE', 'REFUND', 'MANUAL_ADJUSTMENT'")
                .contains("ck_wallet_tx_status CHECK (status IN ('PENDING', 'PROCESSING', 'APPROVED', 'REJECTED'))")
                .contains("ck_wallet_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))")
                .contains("'PENDING_START', 'ACTIVE', 'COMPLETED', 'EXPIRED', 'CANCELLED'")
                .contains("'SYSTEM', 'ATTENDANCE', 'TUITION', 'CLASS_SCHEDULE', 'COACH_TIMESHEET'")
                .doesNotContain("DEDUCT")
                .doesNotContain("tuition_payment")
                .doesNotContain("tuition_payment_detail")
                .doesNotContain("active_context_type");
    }

    @Test
    void v5NormalizesPersonCodesAndAttendanceFk() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V5__normalize_person_codes_and_attendance_fk.sql"));

        assertThat(sql)
                .contains("VQT_SUPER_ADMIN")
                .contains("VQT_SYSTEM_ADMIN")
                .contains("ALTER TABLE core.person ALTER COLUMN person_code SET NOT NULL")
                .contains("ck_person_code_prefix CHECK (")
                .contains("person_code LIKE 'VQ\\_%' OR person_code LIKE 'VQT\\_%'")
                .contains("VALIDATE CONSTRAINT fk_student_attendance_coach_assignment")
                .contains("ADD COLUMN IF NOT EXISTS name VARCHAR(255)");
    }

    @Test
    void v6KeepsScheduleStateOnCourseWithoutAuditTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V6__add_course_pending_schedule.sql"));

        assertThat(sql)
                .contains("next_schedule_id")
                .contains("next_schedule_effective_from")
                .contains("ck_course_next_schedule_pair")
                .contains("uk_class_session_course_date_active")
                .contains("idx_class_session_lifecycle")
                .contains("idx_enrollment_status_period")
                .doesNotContain("course_schedule_change")
                .doesNotContain("course_schedule_impact");
    }
}
