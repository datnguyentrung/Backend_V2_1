package com.dat.backend_v2_1.config;

import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.dat.backend_v2_1.util.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter kiểm tra UserStatus phải là ACTIVE cho mọi API authenticated.
 * Filter này chạy SAU KHI JWT được decode và Authentication đã được set vào SecurityContext.
 */
@Slf4j
@Component
public class UserStatusValidationFilter extends OncePerRequestFilter {

    // Danh sách các endpoint không cần check status (public endpoints)
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // Bỏ qua các endpoint public
        if (isExcludedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Lấy Authentication từ SecurityContext (đã được set bởi JwtAuthenticationFilter trước đó)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Nếu request không authenticated (anonymous) -> Cho qua, để SecurityConfig xử lý authorize
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Request đã authenticated -> Kiểm tra UserStatus
        try {
            String statusString = SecurityUtil.getCurrentUserStatus()
                    .orElseThrow(() -> new RuntimeException("Missing user status in token"));

            UserStatus status = UserStatus.valueOf(statusString);

            // Nếu không phải ACTIVE -> Trả về 403 Forbidden
            if (status != UserStatus.ACTIVE) {
                log.warn("Access denied for user with status: {} on path: {}", status, requestPath);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        String.format("{\"error\":\"Forbidden\",\"message\":\"Tài khoản của bạn chưa được kích hoạt (Status: %s)\"}", status)
                );
                return; // Dừng filter chain, không cho request đi tiếp
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid user status in token for path: {}", requestPath, e);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid User Status in Token\"}");
            return;
        } catch (Exception e) {
            log.error("Error validating user status for path: {}", requestPath, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Internal Server Error\",\"message\":\"Failed to validate user status\"}");
            return;
        }

        // Status hợp lệ (ACTIVE) -> Cho request đi tiếp
        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra xem path có nằm trong danh sách excluded không
     */
    private boolean isExcludedPath(String requestPath) {
        return EXCLUDED_PATHS.stream().anyMatch(requestPath::startsWith);
    }
}

