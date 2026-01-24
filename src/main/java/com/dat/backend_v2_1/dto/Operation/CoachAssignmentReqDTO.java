package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CoachAssignmentReqDTO {
    // =================================================================
    // 1. NHÓM REQUEST (INPUT) - Dữ liệu từ Client gửi lên
    // =================================================================

    /**
     * DTO dùng cho việc TẠO MỚI (Create)
     * Chỉ chứa thông tin cần thiết để bắt đầu một phân công HLV.
     */
    @Data
    public static class CreateRequest {
        @NotNull(message = "Học viên không được để trống")
        private String coachId; // Nhận String (UUID)

        @NotEmpty(message = "Vui lòng chọn ít nhất một lớp học")
        private List<String> scheduleIds; // Nhận String (UUID)

        @NotNull(message = "Ngày phân công không được để trống")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate assignmentDate;

        @NotNull(message = "Ngày kết thúc không được để trống")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        private String note;
    }

    @Data
    public static class UpdateRequest{
        @NotNull(message = "Trạng thái không được để trống")
        private CoachAssignmentStatus status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate assignmentDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        private String note;
    }


}
