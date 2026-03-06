package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CoachAssignmentResDTO {
    /**
     * DTO hiển thị đầy đủ thông tin (Detail)
     * Bao gồm cả thông tin liên kết (Coach, ClassSchedule) đã được flatten (làm phẳng)
     * hoặc nest object nhỏ.
     */
    @Data
    @Builder
    public static class Response {
        private UUID assignmentId;

        // --- INFO OBJECTS (Không trả về cả Entity Coach to đùng) ---
        private CoachResDTO.CoachSummary coach;
        private ClassScheduleResDTO.ClassScheduleSummary classSchedule;

        // --- TIME & STATUS ---
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate assignedDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        private CoachAssignmentStatus status;
        private String note;

        // --- AUDIT ---
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class SimpleResponse {
        private UUID assignmentId;
        private CoachResDTO.CoachSummary coach;
        private ClassScheduleResDTO.ClassScheduleSummary classSchedule;
        private LocalDate assignedDate;
        private CoachAssignmentStatus status;
    }
}
