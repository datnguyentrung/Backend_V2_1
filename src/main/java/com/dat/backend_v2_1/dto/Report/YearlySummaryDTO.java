package com.dat.backend_v2_1.dto.Report;

import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import com.dat.backend_v2_1.enums.Report.ExamEligibility;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

public class YearlySummaryDTO {
    @Data
    @Builder
    @NoArgsConstructor  // <--- BẮT BUỘC: Giúp Jackson khởi tạo được object từ JSON
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class YearlySummaryResponse {
        int year;
        List<QuarterSummary> quarters; // Sẽ chứa 4 phần tử Q1, Q2, Q3, Q4
    }

    @Data
    @Builder
    @NoArgsConstructor  // <--- BẮT BUỘC: Giúp Jackson khởi tạo được object từ JSON
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuarterSummary {
        int quarterNumber; // 1, 2, 3, 4

        StudentAttendanceDTO.AttendanceStats attendanceStats; // Thống kê điểm danh của quý

        // 1. Cột Điểm Chuyên Cần
        double attendanceScore; // 4.5

        // 2. Cột Điểm Chuyên Môn
        double performanceScore; // 76

        // 3. Cột Điểm Thưởng (Nếu có Entity riêng để lưu)
//        double bonusScore;    // 12
//        List<BonusDetail> bonusDetails;

        // 4. Tổng kết
        double totalQuarterScore = attendanceScore + performanceScore; // 92.5

        ExamEligibility eligibility; // ĐỦ ĐIỀU KIỆN / MIỄN THI THỬ
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    @NoArgsConstructor  // <--- BẮT BUỘC: Giúp Jackson khởi tạo được object từ JSON
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuarterSummaryForRedis extends StudentAttendanceDTO.AttendanceStats {
        int quarterNumber; // 1, 2, 3, 4

        // 1. Cột Điểm Chuyên Cần
        double attendanceScore; // 4.5

        // 2. Cột Điểm Chuyên Môn
        double performanceScore; // 76

        // 3. Cột Điểm Thưởng (Nếu có Entity riêng để lưu)
//        double bonusScore;    // 12
//        List<BonusDetail> bonusDetails;

        // 4. Tổng kết
        double totalQuarterScore = attendanceScore + performanceScore; // 92.5

        ExamEligibility eligibility; // ĐỦ ĐIỀU KIỆN / MIỄN THI THỬ
    }
}
