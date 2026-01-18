package com.dat.backend_v2_1.util;

import com.dat.backend_v2_1.domain.Security.Role;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class AccountUtil {
    public static String generateIdAccount(Role role, Integer number) {
        // Thế hệ
        String gen = String.valueOf(getGenCurrent());

        // Nhiệm vụ
        String roleUser = getRoleKey(role);

        // Mã phân biệt người dùng chính
        String idUser = String.format("%03d", number);

        return "TKD" + gen + "G" + roleUser + idUser;
    }

    public static String getGenCurrent() {
        long years = ChronoUnit.YEARS.between(
                LocalDate.of(2012, 5, 15),
                LocalDate.now()
        );
        return String.format("%02d", years);
    }

    public static String getRoleKey(Role role) {
        String roleId = String.valueOf(role.getRoleId());

        // Cần Java 21 (hoặc Java 17-20 bật chế độ preview)
        return switch (roleId) {
            case String s when s.contains("STUDENT") -> "S";
            case String s when s.contains("COACH") -> "C";
            case String s when s.contains("ASSISTANT") -> "A";
            case String s when s.contains("ADMIN") -> "D";
            case null, default -> "N"; // Xử lý null và trường hợp còn lại
        };
    }
}
