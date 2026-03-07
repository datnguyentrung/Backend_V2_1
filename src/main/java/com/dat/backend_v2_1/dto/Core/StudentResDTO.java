package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class StudentResDTO {
    /**
     * DTO trả về danh sách Student kèm theo thống kê số lượng theo từng trạng thái
     * Bao gồm:
     * - activeStudentCount: Số lượng học viên đang học (ACTIVE)
     * - reservedStudentCount: Số lượng học viên đang tạm dừng (RESERVED)
     * - droppedStudentCount: Số lượng học viên đã nghỉ học (DROPPED)
     * - Thông tin phân trang được trích xuất từ Page để tránh warning serialization
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StudentListResponse {
        // Statistics
        long activeStudentCount;
        long reservedStudentCount;
        long droppedStudentCount;

        PageResponse<StudentOverview> students; // Thông tin phân trang và danh sách học viên
    }


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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StudentOverview {
        String studentCode;

        String nationalCode;

        String fullName;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate birthDate;

        String phoneNumber;

        Belt belt;

        String roleName;

        StudentStatus studentStatus;

        String branchName;

        List<ClassScheduleResDTO.ClassScheduleSummary> classSchedules; // Danh sách lịch học của học viên
    }

    /**
     * DTO trả về thông tin tóm tắt Student cho danh sách
     */

    @Data
    @Builder
    public static class StudentSummary {
        private UUID userId;
        private String fullName;
        private String email;
        private String code; // Mã sinh viên
    }
}

