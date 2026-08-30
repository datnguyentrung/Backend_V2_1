package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.service.Training.session.ClassSessionTimingPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClassSessionTimingPolicyTest {
    @Test
    void midpointIsHalfwayThroughTheSession() {
        LocalDateTime close = ClassSessionTimingPolicy.attendanceCloseTime(
                LocalDate.of(2026, 8, 30), LocalTime.of(18, 0), LocalTime.of(19, 30));
        assertThat(close).isEqualTo(LocalDateTime.of(2026, 8, 30, 18, 45));
    }

    @Test
    void invalidOrMissingInputsReturnNull() {
        assertThat(ClassSessionTimingPolicy.attendanceCloseTime(
                LocalDate.of(2026, 8, 30), LocalTime.of(19, 30), LocalTime.of(18, 0))).isNull();
        assertThat(ClassSessionTimingPolicy.attendanceCloseTime(null, LocalTime.of(18, 0),
                LocalTime.of(19, 30))).isNull();
        assertThat(ClassSessionTimingPolicy.attendanceCloseTime(
                LocalDate.of(2026, 8, 30), null, LocalTime.of(19, 30))).isNull();
    }
}
