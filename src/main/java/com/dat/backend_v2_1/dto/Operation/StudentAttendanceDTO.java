package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.EvaluationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StudentAttendanceDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Response {
        UUID attendanceId;
        UUID enrollmentId;

        // Thông tin học viên (Flatten để FE dễ hiển thị)
        UUID studentId;
        String studentName;

        // Thông tin buổi học
        String classScheduleId;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate sessionDate;

        // Trạng thái điểm danh
        AttendanceStatus attendanceStatus;
        LocalDateTime checkInTime;
        String recordedByCoachName; // Tên HLV đã điểm danh

        // Đánh giá
        EvaluationStatus evaluationStatus;
        String note;
        String evaluatedByCoachName; // Tên HLV đã đánh giá

        LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SimpleResponse {
        UUID attendanceId;
        UUID enrollmentId;
        UUID studentId;
        AttendanceStatus attendanceStatus;
        LocalDateTime checkInTime;
        String recordedByCoachName;

        EvaluationStatus evaluationStatus;
        String evaluatedByCoachName;
        String note;
    }

    // ========================================================================
    // REQUEST DTOs (Dữ liệu Frontend gửi lên)
    // ========================================================================

    /**
     * DTO dùng cho API "Điểm danh nhanh" (Mark All Present)
     * Hoặc tạo mới một danh sách điểm danh
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BatchCreateRequest {
        @NotNull(message = "Schedule ID không được để trống")
        String classScheduleId;

        @NotNull(message = "Ngày học không được để trống")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate sessionDate;
    }

    /**
     * DTO: ManualLogRequest
     * Tiếng Việt: Yêu cầu điểm danh thủ công (cho 1 học viên)
     * * Mục đích: Dùng khi Admin muốn tạo trước dữ liệu xin nghỉ (EXCUSED),
     * hoặc HLV muốn sửa trạng thái của riêng 1 học viên cụ thể.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ManualLogRequest {
        @NotNull(message = "Phải chọn học viên")
        UUID studentId; // Bắt buộc phải biết tạo cho ai

        @NotNull(message = "Phải chọn lớp")
        String classScheduleId;

        @NotNull(message = "Ngày xin phép/đi học")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate sessionDate;

        @NotNull
        AttendanceStatus attendanceStatus; // Thường là EXCUSED (Có phép) hoặc PRESENT (Đi bù)

        LocalDateTime checkInTime; // Nullable. Nếu xin nghỉ (EXCUSED/ABSENT) thì để null. Nếu đi học thì truyền vào.

        @Size(max = 500)
        String note; // Lý do: "Về quê", "Ốm", ...
    }

    /**
     * DTO dùng để cập nhật trạng thái điểm danh của 1 học viên (PATCH)
     * Ví dụ: Sửa từ Vắng -> Có mặt, hoặc Đi muộn
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UpdateStatusRequest {
        @NotNull(message = "Trạng thái điểm danh không được để trống")
        AttendanceStatus attendanceStatus;

        // Lưu ý: Không truyền coachId ở đây để bảo mật.
        // Backend sẽ tự lấy ID của HLV đang đăng nhập từ Token.
    }

    /**
     * DTO dùng để cập nhật đánh giá/nhận xét (PATCH)
     * Tách riêng vì đôi khi HLV chấm điểm sau giờ học
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UpdateEvaluationRequest {
        EvaluationStatus evaluationStatus;

        @Size(max = 500, message = "Ghi chú không được quá 500 ký tự")
        String note;
    }

    /**
     * DTO tổng hợp nếu muốn update cả 2 cùng lúc (ít dùng hơn nhưng nên có)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FullUpdateRequest {
        AttendanceStatus attendanceStatus;
        EvaluationStatus evaluationStatus;
        String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CreateRequest {
        UUID studentId;
    }
}
