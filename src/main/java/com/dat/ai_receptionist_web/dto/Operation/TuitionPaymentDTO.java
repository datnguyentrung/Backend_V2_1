package com.dat.ai_receptionist_web.dto.Operation;

import com.dat.ai_receptionist_web.dto.Core.StudentResDTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TuitionPaymentDTO {

    /**
     * Request: Đóng học phí cho 1 enrollment cụ thể
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ProcessPaymentRequest {
        @NotNull(message = "Mã học viên không được để trống")
        UUID studentId;

        @NotNull(message = "Mã đăng ký lớp không được để trống")
        UUID enrollmentId;

        @Min(value = 1, message = "Số tháng phải ít nhất là 1")
        int numberOfMonths;

        String note;
    }

    /**
     * Response: Thông tin hóa đơn sau khi tạo thành công
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TuitionPaymentResponse {
        UUID paymentId;
        StudentResDTO.StudentSummary student;
        BigDecimal totalAmount;
        String note;
        LocalDateTime createdAt;
        List<TuitionPaymentDetailResponse> details;
    }

    /**
     * Response: Lịch sử đóng phí (join Payment + Detail)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TuitionPaymentDetailResponse {
        UUID detailId;
        UUID enrollmentId;
        String scheduleId;
        int forMonth;
        int forYear;
        BigDecimal amountAllocated;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TuitionStatusResponse {
        UUID studentId;
        String studentCode;
        String fullName;
        boolean hasPaidCurrentMonth;
        int currentMonth;
        int currentYear;
        List<ActiveClassStatus> activeClasses;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ActiveClassStatus {
        UUID enrollmentId;
        String scheduleId;
        boolean paid;
        BigDecimal amountAllocated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PaymentHistoryItem {
        int forMonth;
        int forYear;
        BigDecimal amountAllocated;
        String className;   // scheduleId của lớp
        LocalDateTime paidAt;     // createdAt của Payment cha
    }
}
