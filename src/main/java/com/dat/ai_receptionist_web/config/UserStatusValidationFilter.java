package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class UserStatusValidationFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/auth/mobile/login",
            "/api/v1/auth/mobile/logout",
            "/api/v1/auth/mobile/refresh"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (isExcludedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isSystemAuthentication(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String statusString = SecurityUtil.getCurrentUserStatus()
                    .orElseThrow(() -> new RuntimeException("Missing user status in token"));
            UserStatus status = UserStatus.valueOf(statusString);
            if (status != UserStatus.ACTIVE) {
                forbidden(response, "Account is not active");
                return;
            }

            if (!requestPath.startsWith("/api/v1/auth/") && authentication.getPrincipal() instanceof Jwt jwt) {
                if (jwt.getClaim("activePersonId") == null || jwt.getClaim("activeContextType") == null) {
                    forbidden(response, "Active context is required");
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Security token validation failed on path {}", requestPath, e);
            forbidden(response, "Invalid security token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSystemAuthentication(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority != null && authority.contains("SYSTEM"));
    }

    private boolean isExcludedPath(String requestPath) {
        return EXCLUDED_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    private void forbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" + message + "\"}");
    }
}
