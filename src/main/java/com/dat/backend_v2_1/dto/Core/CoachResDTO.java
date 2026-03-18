package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CoachResDTO {

    /**
     * DTO trả về thông tin chi tiết Coach
     * Bao gồm thông tin từ Coach và User (parent class)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoachDetail {
        // === Thông tin từ User (Base Entity) ===
        private UUID userId;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate birthDate;

        private String phoneNumber;

        private Belt belt;

        private UserStatus status; // Trạng thái tài khoản hệ thống (ACTIVE, BANNED, etc.)

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime createdAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime updatedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime lastLoginAt;

        private String roleName; // Tên role (COACH, ADMIN, etc.)

        // === Thông tin từ Coach (Child Entity) ===
        private String staffCode;

        private String fullName;

        private CoachStatus coachStatus; // Trạng thái công việc (ACTIVE, ON_LEAVE, etc.)
    }

    /**
     * DTO trả về thông tin tóm tắt Coach cho danh sách
     */
    @Data
    @Builder
    public static class CoachSummary {
        private UUID userId;
        private String fullName;
        private String staffCode;
    }
}
