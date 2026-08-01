package com.dat.ai_receptionist_web.dto.Operation;

import com.dat.ai_receptionist_web.dto.Core.ClassScheduleResDTO;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CoachAssignmentResDTO {
    /** Internal, canonical payload persisted in the single Coach Assignment cache. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheResponse {
        private UUID assignmentId;
        private CoachResDTO.CoachSummary coach;
        private ClassScheduleResDTO.ClassScheduleSummary classSchedule;
        private LocalDate assignedDate;
        private LocalDate endDate;
        private CoachAssignmentStatus status;
        private String note;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

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
//        private CoachResDTO.CoachSummary coach;
        private ClassScheduleResDTO.ClassScheduleSummary classSchedule;

        // --- TIME & STATUS ---
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate assignedDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        private CoachAssignmentStatus status;
        private String note;

        // --- AUDIT ---
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    public static class SimpleResponse {
        private UUID assignmentId;
        private CoachResDTO.CoachSummary coach;
        private ClassScheduleResDTO.ClassScheduleSummary classSchedule;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate assignedDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
        private CoachAssignmentStatus status;
    }
}
