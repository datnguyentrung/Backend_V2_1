package com.dat.backend_v2_1.util;

import com.dat.backend_v2_0.domain.authz.Roles;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class AccountUtil {
    public static String generateIdAccount(Roles role, Integer number) {
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

    public static String getRoleKey(Roles role) {
        return switch (role.getIdRole()) {
            case "STUDENT" -> "S";
            case "COACH" -> "C";
            case "ASSISTANT" -> "A";
            case "ADMIN" -> "D";
            default -> "N";
        };
    }
}
