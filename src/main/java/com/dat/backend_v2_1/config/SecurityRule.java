package com.dat.backend_v2_1.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("securityRule")
public class SecurityRule {
    // 1. Cấp cao nhất: HEAD_COACH (Tôi thêm cả ADMIN để đề phòng hệ thống bạn có Admin tổng)
    public boolean isHeadCoach(Authentication authentication) {
        return checkContains(authentication, "HEAD_COACH")
                || checkContains(authentication, "ADMIN");
    }

    // 2. Cấp trung: MANAGER
    // Trả về true nếu user có chữ MANAGER, HOẶC user đó là HEAD_COACH
    public boolean isManager(Authentication authentication) {
        return checkContains(authentication, "MANAGER")
                || isHeadCoach(authentication);
    }

    // 3. Cấp cơ sở: COACH
    // Trả về true nếu user có chữ COACH, HOẶC user đó thỏa mãn điều kiện isManager
    public boolean isCoach(Authentication authentication) {
        return checkContains(authentication, "COACH")
                || isManager(authentication);
    }

    // --- Hàm Helper dùng chung để tránh lặp code ---
    private boolean checkContains(Authentication authentication, String keyword) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority() != null && a.getAuthority().contains(keyword));
    }
}