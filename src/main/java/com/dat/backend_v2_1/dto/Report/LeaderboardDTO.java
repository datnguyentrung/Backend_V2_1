package com.dat.backend_v2_1.dto.Report;

import com.dat.backend_v2_1.enums.Core.Belt;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

public class LeaderboardDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Response {
        int year;
        int quarter;
        int totalStudents; // Tổng số học viên có trong bảng xếp hạng
        List<RankItem> rankings;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RankItemForRedis extends YearlySummaryDTO.QuarterSummaryForRedis {
        int rank;            // Hạng (1, 2, 3...)
        String studentCode;  // Mã HV (Dùng để FE làm key hoặc link tới trang chi tiết)
        String fullName;     // Tên HV
        Belt belt;     // Cấp đai (Rất cần để FE render màu đai tương ứng)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RankItem {
        int rank;            // Hạng (1, 2, 3...)
        String studentCode;  // Mã HV (Dùng để FE làm key hoặc link tới trang chi tiết)
        String fullName;     // Tên HV
        Belt belt;     // Cấp đai (Rất cần để FE render màu đai tương ứng)
        YearlySummaryDTO.QuarterSummary quarterSummary; // Thông tin chi tiết về điểm số và thống kê của quý
    }
}
