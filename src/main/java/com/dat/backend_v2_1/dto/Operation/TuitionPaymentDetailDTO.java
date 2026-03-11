package com.dat.backend_v2_1.dto.Operation;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class TuitionPaymentDetailDTO {

    /**
     * Response cho từng dòng phân bổ kỳ hạn
     */
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TuitionPaymentDetailResponse {
        UUID detailId;
        UUID enrollmentId;
        String scheduleId;      // Mã lớp để hiển thị
        int forMonth;
        int forYear;
        BigDecimal amountAllocated;
    }

    /**
     * Response cho tác vụ kiểm tra trạng thái học phí (Check-in gate)
     */
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
        boolean hasPaidCurrentMonth;   // true → cho qua cửa
        int currentMonth;
        int currentYear;
        List<ActiveClassStatus> activeClasses;
    }

    /**
     * Trạng thái học phí theo từng lớp
     */
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ActiveClassStatus {
        UUID enrollmentId;
        String scheduleId;
        boolean paid;               // Đã đóng tháng hiện tại
        BigDecimal amountAllocated; // null nếu chưa đóng
    }
}
