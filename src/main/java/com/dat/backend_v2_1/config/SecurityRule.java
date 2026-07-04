package com.dat.backend_v2_1.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("securityRule")
public class SecurityRule {

    /**
     * Quyền hệ thống: dùng cho các request chạy qua X-API-KEY
     * Ví dụ: Python AI service, cron job, webhook, sync batch...
     */
    public boolean isSystem(Authentication authentication) {
        return checkContains(authentication, "SYSTEM");
    }

    /**
     * Cấp cao nhất: HEAD_COACH / DEVELOPER
     */
    public boolean isHeadCoach(Authentication authentication) {
        return checkContains(authentication, "HEAD_COACH")
                || checkContains(authentication, "DEVELOPER");
    }

    /**
     * Cấp trung: MANAGER_SENIOR
     * HEAD_COACH và DEVELOPER cũng được xem là manager senior.
     */
    public boolean isManagerSenior(Authentication authentication) {
        return checkContains(authentication, "MANAGER_SENIOR")
                || isHeadCoach(authentication);
    }

    /**
     * Cấp cơ sở: COACH
     * MANAGER, MANAGER_SENIOR, HEAD_COACH, DEVELOPER đều có quyền như COACH.
     */
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

    /**
     * Helper kiểm tra quyền.
     * <p>
     * Hỗ trợ cả dạng:
     * - COACH
     * - ROLE_COACH
     * - MANAGER
     * - ROLE_MANAGER
     * - SYSTEM
     * - ROLE_SYSTEM
     */
    private boolean checkContains(Authentication authentication, String keyword) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(auth -> auth.contains(keyword));
    }
}