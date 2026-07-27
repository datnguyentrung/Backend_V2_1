package com.dat.ai_receptionist_web.dto.Operation;

import com.dat.ai_receptionist_web.dto.Core.ClassScheduleResDTO;
import com.dat.ai_receptionist_web.enums.Operation.SessionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClassSessionDTO { // 1. Bỏ @Data ở class ngoài cùng

    @Data
    @Builder
    @NoArgsConstructor  // <--- BẮT BUỘC: Giúp Jackson khởi tạo được object từ JSON
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SessionCreateRequest {
        @NotNull(message = "Mã lịch học không được để trống")
        String scheduleId;

        LocalDate sessionDate;
        LocalTime startTime;
        LocalTime endTime;
        String note;

        @Builder.Default
        SessionStatus status = SessionStatus.ACTIVE;

        // 2. Không cần @Builder.Default cho false vì kiểu primitive boolean mặc định đã là false
        boolean isAttendanceClosed;
    }

    @Data
    @Builder
    @NoArgsConstructor  // <--- BẮT BUỘC: Giúp Jackson khởi tạo được object từ JSON
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SessionUpdateRequest {
        LocalDate sessionDate;
        LocalTime startTime;
        LocalTime endTime;
        String note;

        // 3. Bỏ giá trị mặc định ở Update Request
        SessionStatus status;

        // 4. Dùng class Wrapper 'Boolean' thay vì primitive 'boolean'
        Boolean isAttendanceClosed;
    }

    @Data
    @Builder
    @NoArgsConstructor  // <--- BẮT BUỘC: Giúp Jackson khởi tạo được object từ JSON
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SessionResponse {
        UUID sessionId; // 5. Đổi thành UUID cho đồng bộ với Entity
        LocalDate sessionDate;
        LocalTime startTime;
        LocalTime endTime;
        String note;
        SessionStatus status;
        boolean isAttendanceClosed;

        ClassScheduleResDTO.ClassScheduleSummary classSchedule;
    }

    public interface ReportSessionRow {
        UUID getSessionId();

        LocalDate getSessionDate();

        String getClassScheduleId();

        String getBranchName();

        LocalTime getStartTime();

        LocalTime getEndTime();
    }

    public record ReportData(
            ReportSessionRow session,
            StudentAttendanceDTO.AttendanceStats attendanceStats,
            List<ResponsibleCoachProjection> responsibleCoaches,
            Map<UUID, CoachTimesheetStatusProjection> timesheetsByCoachId
    ) {
    }

    public record ReportPayload(
            UUID sessionId,
            String classScheduleId,
            String title,
            String body,
            Map<String, String> data,
            Set<UUID> coachPersonIds
    ) {
    }

    public record CompletedNotificationMessage(UUID sessionId) {
    }
}
