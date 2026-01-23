package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class StudentEnrollmentResDTO {
    /**
     * DTO hiển thị đầy đủ thông tin (Detail)
     * Bao gồm cả thông tin liên kết (Student, Class) đã được flatten (làm phẳng)
     * hoặc nest object nhỏ.
     */
    @Data
    @Builder
    public static class Response {
        private UUID enrollmentId;

        // --- INFO OBJECTS (Không trả về cả Entity Student to đùng) ---
        private StudentResDTO.StudentSummary student;
        private ClassScheduleResDTO.ClassScheduleSummary classSchedule;

        // --- TIME & STATUS ---
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate joinDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate leaveDate;

        private StudentEnrollmentStatus status;
        private String note;

        // --- AUDIT ---
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class SimpleResponse {
        private UUID enrollmentId;
        private ClassScheduleResDTO.ClassScheduleSummary classScheduleSummary;
        private LocalDate joinDate;
        private StudentEnrollmentStatus status;
    }
}
