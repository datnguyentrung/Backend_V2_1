package com.dat.backend_v2_1.util;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

public class ConverterUtils {
    public static LocalDate instantToLocalDate(Instant instant) {
        return instant.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
    }

    public static LocalDate localDateTimeToLocalDate(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate();
    }

    public static List<Integer> getMonthsByQuarter(int quarter) {
        return switch (quarter) {
            case 1 -> List.of(1, 2, 3);
            case 2 -> List.of(4, 5, 6);
            case 3 -> List.of(7, 8, 9);
            case 4 -> List.of(10, 11, 12);
            default -> List.of();
        };
    }

    // Hàm này chuyển "Nguyễn Văn A" -> "Nguyen Van A"
    public static String removeAccents(String input) {
        if (input == null) return null;

        // Chuẩn hóa Unicode tổ hợp
        String temp = Normalizer.normalize(input, Normalizer.Form.NFD);

        // Xóa các dấu thanh (sắc, huyền, hỏi, ngã, nặng)
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
