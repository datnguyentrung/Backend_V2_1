package com.dat.backend_v2_1.util.Helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validation {
    // 1. Regex cho số điện thoại:
    // ^0     : Bắt đầu bằng số 0
    // \\d{9} : Theo sau là đúng 9 chữ số nữa (0-9)
    // $      : Kết thúc chuỗi
    private static final String PHONE_PATTERN = "^0\\d{9}$";

    // 2. Regex cho Email (Tiêu chuẩn OWASP Validation Regex đơn giản hóa)
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    // Compile Pattern trước để tối ưu hiệu suất (tránh compile lại mỗi lần gọi hàm)
    private static final Pattern phoneRegex = Pattern.compile(PHONE_PATTERN);
    private static final Pattern emailRegex = Pattern.compile(EMAIL_PATTERN);

    /**
     * Kiểm tra số điện thoại hợp lệ (10 số, bắt đầu bằng 0)
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) {
            return false;
        }
        Matcher matcher = phoneRegex.matcher(phone);
        return matcher.matches();
    }

    /**
     * Kiểm tra Email hợp lệ
     */
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        Matcher matcher = emailRegex.matcher(email);
        return matcher.matches();
    }
}
