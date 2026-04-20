package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CoachDetail {
        // === Thông tin từ User (Base Entity) ===
        UUID userId;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate birthDate;

        String phoneNumber;

        Belt belt;

        String email;

        UserStatus status; // Trạng thái tài khoản hệ thống (ACTIVE, BANNED, etc.)

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        LocalDateTime createdAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        LocalDateTime updatedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        LocalDateTime lastLoginAt;

        String roleName; // Tên role (COACH, ADMIN, etc.)

        // === Thông tin từ Coach (Child Entity) ===
        String staffCode;

        String fullName;

        CoachStatus coachStatus; // Trạng thái công việc (ACTIVE, ON_LEAVE, etc.)

        List<CoachAssignmentResDTO.SimpleResponse> currentAssignments; // Thông tin phân công hiện tại (nếu có)
    }

    /**
     * DTO trả về thông tin tóm tắt Coach cho danh sách
     */
    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CoachSummary {
        UUID userId;
        String fullName;
        String staffCode;
        String email;
    }
}
