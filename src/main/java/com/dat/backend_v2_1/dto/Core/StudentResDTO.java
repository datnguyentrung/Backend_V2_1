package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class StudentResDTO {

    /**
     * DTO trả về thông tin chi tiết Student
     * Bao gồm thông tin từ Student và User (parent class)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentDetail {
        // === Thông tin từ User (Base Entity) ===
        private UUID userId;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate birthDate;

        private String phoneNumber;

        private Belt belt;

        private UserStatus status; // Trạng thái tài khoản hệ thống (ACTIVE, BANNED, etc.)

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private Instant createdAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private Instant updatedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private Instant lastLoginAt;

        private String roleName; // Tên role (STUDENT, TEACHER, ADMIN, etc.)

        // === Thông tin từ Student (Child Entity) ===
        private String studentCode;

        private String nationalCode; // CCCD/CMND

        private String fullName;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        private StudentStatus studentStatus; // Trạng thái học tập (ACTIVE, RESERVED, DROPPED)

        // === Thông tin Branch (Related Entity) ===
        private Long branchId;

        private String branchName;

        private String branchAddress;
    }

    /**
     * DTO trả về thông tin tóm tắt Student cho danh sách
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private UUID userId;
        private String studentCode;
        private String fullName;
        private String phoneNumber;
        private Belt belt;
        private StudentStatus studentStatus;
        private String branchName;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate startDate;
    }
}

