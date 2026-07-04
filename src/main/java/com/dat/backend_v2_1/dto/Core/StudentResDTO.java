package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class StudentResDTO {
    // ĐÂY LÀ INTERFACE PROJECTION (Gọn gàng nằm chung 1 file)
    public interface StudentRankInfo {
        String getStudentCode();

        String getFullName();

        Belt getBelt();
    }

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
    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentDetail extends UserRes.UserDetail {
        private String studentCode;

        private String nationalCode; // CCCD/CMND

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        private StudentStatus studentStatus; // Trạng thái học tập (ACTIVE, RESERVED, DROPPED)

        // === Thông tin Branch (Related Entity) ===
        private Long branchId;

        private String branchName;

        private String branchAddress;

        // === Thông tin Enrollment (Related Entities) ===
        private List<StudentEnrollmentResDTO.SimpleResponse> enrollments;
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
    @NoArgsConstructor // Quan trọng: Bắt buộc phải có để Jackson làm việc
    @AllArgsConstructor
    public static class StudentSummary {
        private UUID userId;
        private String fullName;
        //        private String email;
        private String code; // Mã sinh viên
        private Belt belt;
    }
}

