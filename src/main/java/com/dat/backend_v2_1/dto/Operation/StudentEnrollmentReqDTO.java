package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class StudentEnrollmentReqDTO {
    // =================================================================
    // 1. NHÓM REQUEST (INPUT) - Dữ liệu từ Client gửi lên
    // =================================================================

    /**
     * DTO dùng cho việc TẠO MỚI (Create)
     * Chỉ chứa thông tin cần thiết để bắt đầu một enrollment.
     */
    @Data
    public static class CreateRequest {
        @NotNull(message = "Học viên không được để trống")
        private String studentId; // Nhận String (UUID)

        @NotEmpty(message = "Vui lòng chọn ít nhất một lớp học")
        private List<String> scheduleIds; // Hỗ trợ đăng ký nhiều lớp 1 lúc

        @NotNull(message = "Ngày nhập học không được để trống")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate joinDate;

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        private String note;
    }

    /**
     * DTO dùng cho việc CẬP NHẬT (Update)
     * Thường chỉ cập nhật trạng thái, ngày nghỉ hoặc ghi chú.
     * Không cho phép sửa studentId hay scheduleId (muốn sửa phải xóa đi tạo lại).
     */
    @Data
    public static class UpdateRequest {
        @NotNull(message = "Trạng thái không được để trống")
        private StudentEnrollmentStatus status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate leaveDate; // Có thể null nếu quay lại trạng thái ACTIVE

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate joinDate; // Cho phép cập nhật ngày nhập học nếu cần

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        private String note;
    }
}
