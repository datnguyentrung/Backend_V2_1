package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.enums.Core.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class ClassScheduleResDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ClassScheduleSummary {
        String scheduleId;
        String branchName;
        ScheduleLocation scheduleLocation;
        ScheduleLevel scheduleLevel;
        ScheduleShift scheduleShift;

        // Format thời gian gọn gàng: "08:30" thay vì "08:30:00"
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime;

        Weekday weekday; // Enum (MONDAY, TUESDAY...)
    }

    /**
     * DTO chi tiết: Dùng khi xem chi tiết 1 lớp học
     * Chứa đầy đủ thông tin để hiển thị UI.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các field null
    public static class ClassScheduleDetail {
        String scheduleId;

        // --- Thông tin Chi nhánh (Flatten object thay vì trả về ID trơ trọi) ---
        Long branchId;
        String branchName;

        // --- Thông tin HLV (Danh sách vì có thể có nhiều HLV dạy chung 1 lớp) ---
        List<CoachResDTO.CoachSummary> coaches;

        // --- Thông tin Lớp học ---
        ScheduleLevel scheduleLevel;      // Trình độ (BEGINNER, INTERMEDIATE...)
        ScheduleShift scheduleShift;      // Ca học (MORNING, EVENING...)
        ScheduleLocation scheduleLocation;              // Phòng học (Location)
        ScheduleStatus scheduleStatus;              // Trạng thái (ACTIVE, INACTIVE...)

        // --- Thời gian ---
        Weekday weekday;

//        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime;

//        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime;

        // --- Metadata (Thống kê) ---
        Integer totalStudents;    // Sĩ số hiện tại (Calculated field)
    }
}
