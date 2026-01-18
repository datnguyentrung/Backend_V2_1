package com.dat.backend_v2_1.enums.Core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum Weekday {

    MONDAY(2, "Thứ Hai"),
    TUESDAY(3, "Thứ Ba"),
    WEDNESDAY(4, "Thứ Tư"),
    THURSDAY(5, "Thứ Năm"),
    FRIDAY(6, "Thứ Sáu"),
    SATURDAY(7, "Thứ Bảy"),
    SUNDAY(1, "Chủ Nhật"); // Lưu ý: Một số hệ thống quy định CN là 8 hoặc 0, ở đây giữ nguyên là 1 theo yêu cầu

    private final int code;      // Giá trị lưu trong DB
    private final String label;  // Giá trị hiển thị UI

    // --- CẤU HÌNH TRA CỨU NHANH (Caching) ---
    // Tạo Map tĩnh để tra cứu O(1) thay vì dùng for-loop mỗi lần gọi
    private static final Map<Integer, Weekday> LOOKUP_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(Weekday::getCode, Function.identity()));

    /**
     * Lấy Enum từ số int (Dùng khi convert từ DB lên Java)
     * @param code số thứ tự (2, 3, ..., 1)
     * @return Weekday
     */
    @JsonCreator // Hỗ trợ nhận JSON là số (ví dụ: { "day": 2 })
    public static Weekday fromCode(int code) {
        return Optional.ofNullable(LOOKUP_MAP.get(code))
                .orElseThrow(() -> new IllegalArgumentException("Invalid Weekday code: " + code));
    }

    /**
     * Convert từ java.time.DayOfWeek chuẩn của Java sang Enum này
     */
    public static Weekday fromJavaDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> MONDAY;
            case TUESDAY -> TUESDAY;
            case WEDNESDAY -> WEDNESDAY;
            case THURSDAY -> THURSDAY;
            case FRIDAY -> FRIDAY;
            case SATURDAY -> SATURDAY;
            case SUNDAY -> SUNDAY;
        };
    }

    /**
     * Trả về giá trị code khi serialize ra JSON
     */
    @JsonValue
    public int getCode() {
        return code;
    }
}
