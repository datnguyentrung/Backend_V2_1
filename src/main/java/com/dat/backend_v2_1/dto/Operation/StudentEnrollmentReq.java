package com.dat.backend_v2_1.dto.Operation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentEnrollmentReq {
    @Data
    public static class StudentEnrollmentCreate {
        // Fields for creating a student enrollment can be added here
        @NotBlank(message = "Mã học viên không được để trống")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "ID học viên không đúng định dạng")
        private String studentId;

        @NotBlank(message = "Vui lòng chọn lớp học")
        private String scheduleId;

        @NotNull(message = "Ngày bắt đầu không được để trống")
        private LocalDate joinDate;
    }
}
