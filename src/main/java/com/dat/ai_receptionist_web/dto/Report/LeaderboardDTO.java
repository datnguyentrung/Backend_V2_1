package com.dat.ai_receptionist_web.dto.Report;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

public class LeaderboardDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Response<T> {
        int year;
        int quarter;
        int totalStudents; // Tổng số học viên có trong bảng xếp hạng
        List<RankItem<T>> rankings;
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
    public static class RankItem<T> {
        int rank;            // Hạng (1, 2, 3...)
        Integer rankBefore;
        UUID personId;
        String avatarUrl;
        String studentCode;  // Mã HV (Dùng để FE làm key hoặc link tới trang chi tiết)
        String fullName;     // Tên HV
        Belt belt;     // Cấp đai (Rất cần để FE render màu đai tương ứng)
        T data; // Thông tin chi tiết về điểm số và thống kê của quý
    }
}
