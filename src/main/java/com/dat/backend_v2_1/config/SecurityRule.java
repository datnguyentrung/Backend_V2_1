package com.dat.backend_v2_1.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("securityRule")
public class SecurityRule {
    // 1. Cấp cao nhất: HEAD_COACH (Tôi thêm cả ADMIN để đề phòng hệ thống bạn có Admin tổng)
    public boolean isHeadCoach(Authentication authentication) {
        return checkContains(authentication, "HEAD_COACH")
                || checkContains(authentication, "DEVELOPER");
    }

    // 2. Cấp trung: MANAGER
    // Trả về true nếu user có chữ MANAGER, HOẶC user đó là HEAD_COACH
    public boolean isManagerSenior(Authentication authentication) {
        return checkContains(authentication, "MANAGER_SENIOR")
                || isHeadCoach(authentication);
    }

    // 3. Cấp cơ sở: COACH
    // Trả về true nếu user có chữ COACH, HOẶC user đó thỏa mãn điều kiện isManagerSenior
    public boolean isCoach(Authentication authentication) {
        return checkContains(authentication, "COACH")
                || checkContains(authentication, "MANAGER")
                || isManagerSenior(authentication);
    }

    public boolean isStudent(Authentication authentication) {
        return checkContains(authentication, "STUDENT");
    }

    public boolean isParent(Authentication authentication) {
        return checkContains(authentication, "PARENT");
    }

    // --- Hàm Helper dùng chung để tránh lặp code ---
    private boolean checkContains(Authentication authentication, String keyword) {
        if (authentication == null) {
            return false;
        } else {
            authentication.getAuthorities();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(auth -> auth.contains(keyword)); // Hoặc .equals() tùy bạn
    }
}