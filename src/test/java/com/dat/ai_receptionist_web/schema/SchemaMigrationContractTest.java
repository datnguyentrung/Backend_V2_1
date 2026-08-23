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
}
