package com.dat.backend_v2_1.util;

import com.dat.backend_v2_1.domain.Security.Role;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class AccountUtil {
    public static String getUserCode(String fullName, LocalDate birthDate) {
        if (fullName == null || birthDate == null) return "";

        // BƯỚC 1: Xử lý Tiếng Việt (Xóa dấu + chuyển đ/Đ thành d)
        String temp = Normalizer.normalize(fullName, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        temp = pattern.matcher(temp).replaceAll("");
        temp = temp.replace('đ', 'd').replace('Đ', 'd');

        // BƯỚC 2: Xóa ký tự đặc biệt, chuyển thường và tách mảng
        // Input: "Nguyễn Trung   Đạt" -> parts = ["nguyen", "trung", "dat"]
        String[] nameParts = temp.replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim()
                .toLowerCase()
                .split("\\s+");

        if (nameParts.length == 0) return "";

        // BƯỚC 3: Lấy tên (Phần tử cuối cùng) -> "dat"
        String firstName = nameParts[nameParts.length - 1];

        // BƯỚC 4: Lấy chữ cái đầu của Họ và Tên đệm -> "nt"
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < nameParts.length - 1; i++) {
            if (!nameParts[i].isEmpty()) {
                initials.append(nameParts[i].charAt(0));
            }
        }

        // BƯỚC 5: Format ngày sinh -> "311005"
        String birthDateStr = birthDate.format(DateTimeFormatter.ofPattern("ddMMyy"));

        // BƯỚC 6: Ghép chuỗi theo format HV_tenhodem_ngaysinh
        // Kết quả: "HV_" + "dat" + "nt" + "_" + "311005"
        return "VQ_" + firstName + initials + "_" + birthDateStr;
    }

    public static String getRoleKey(Role role) {
        String roleCode = role.getCode();

        // Cần Java 21 (hoặc Java 17-20 bật chế độ preview)
        return switch (roleCode) {
            case String s when s.contains("STUDENT") -> "S";
            case String s when s.contains("COACH") -> "C";
            case String s when s.contains("ASSISTANT") -> "A";
            case String s when s.contains("ADMIN") -> "D";
            case null, default -> "N"; // Xử lý null và trường hợp còn lại
        };
    }
}
