package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Operation.CoachTimesheetStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Data
public class CoachTimesheetDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ScanRequest {
        @NotBlank(message = "Mã nhân viên không được để trống")
        String staffCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AdjustRequest {
        CoachTimesheetStatus status;
        LocalDateTime checkInTime;
        LocalDateTime checkOutTime;
        @Size(max = 500)
        String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Response {
        UUID timesheetId;
        UUID coachAssignmentId;
        CoachResDTO.CoachSummary coach;
        ClassScheduleResDTO.ClassScheduleSummary classSchedule;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate workingDate;
        LocalDateTime checkInTime;
        LocalDateTime checkOutTime;
        CoachTimesheetStatus status;
        String note;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TimesheetListResponse {
        SummaryResponse summary;
        PageResponse<Response> timesheets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SummaryResponse {
        long totalRecords;
        long totalTeachingSessions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FilterRequest {
        UUID coachId;
        UUID coachAssignmentId;
        String classScheduleId;
        Integer branchId;
        CoachTimesheetStatus status;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate workDate;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fromDate;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate toDate;
        Integer month;
        Integer year;
        String search;

        public YearMonth getYearMonth() {
            if (month == null || year == null) {
                return null;
            }
            return YearMonth.of(year, month);
        }
    }
}
