package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.enums.Core.*;
import com.dat.ai_receptionist_web.enums.Core.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ClassScheduleReqDTO {

    /**
     * DTO dùng cho việc TẠO MỚI lịch học (Create)
     */
    @Data
    public static class CreateRequest {
        @NotBlank(message = "Mã lịch học không được để trống")
        @Size(max = 5, message = "Mã lịch học tối đa 5 ký tự")
        @Pattern(regexp = "^[A-Z0-9]+$", message = "Mã lịch học chỉ được chứa chữ in hoa và số")
        private String scheduleId;

        @NotNull(message = "Chi nhánh không được để trống")
        @Positive(message = "ID chi nhánh không hợp lệ")
        private Long branchId;

        @NotNull(message = "Thứ không được để trống")
        private Weekday weekday;

        @NotNull(message = "Trình độ không được để trống")
        private ScheduleLevel level;

        @NotNull(message = "Giờ bắt đầu không được để trống")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @NotNull(message = "Giờ kết thúc không được để trống")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        @NotNull(message = "Ca học không được để trống")
        private ScheduleShift shift;

        @NotNull(message = "Vị trí không được để trống")
        private ScheduleLocation location;

        @NotNull(message = "Trạng thái không được để trống")
        private ScheduleStatus scheduleStatus;

        @NotNull(message = "Học phí tháng không được để trống")
        private BigDecimal monthlyFee;

        private BigDecimal quarterlyFee; // Có thể dùng để giảm giá khi đóng 3 tháng
    }

    /**
     * DTO dùng cho việc CẬP NHẬT lịch học (Update)
     * Các field có thể null sẽ không được cập nhật
     */
    @Data
    public static class UpdateRequest {
        @Positive(message = "ID chi nhánh không hợp lệ")
        private Long branchId;

        private Weekday weekday;

        private ScheduleLevel level;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        private ScheduleShift shift;

        private ScheduleLocation location;

        private ScheduleStatus scheduleStatus;

        @Positive(message = "Học phí tháng phải lớn hơn 0")
        private BigDecimal monthlyFee;

        @Positive(message = "Học phí quý phải lớn hơn 0")
        private BigDecimal quarterlyFee;
    }
}

