package com.dat.ai_receptionist_web.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.base64-secret}")
    private String expectedApiKey;

    // 🚀 BƯỚC 1: KHAI BÁO DANH SÁCH CÁC API DÙNG X-API-KEY (WEBHOOK, AI, CRON JOB...)
    private static final List<String> API_KEY_ENDPOINTS = List.of(
            "/api/v1/student-attendances/check-in",
            "/api/v1/persons/face-check-in",
            "/api/v1/leaderboards/sync-batch" // <-- Đã thêm API đồng bộ thi đua vào đây
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Bỏ qua nếu là request OPTIONS (Pre-flight của CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 🚀 BƯỚC 2: KIỂM TRA TRONG DANH SÁCH
        // Nếu API đang gọi KHÔNG NẰM TRONG danh sách trên -> Bỏ qua Filter này (return true) để JWT xử lý
        return !API_KEY_ENDPOINTS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestApiKey = request.getHeader("X-API-KEY");

        // Không gửi X-API-KEY => cho đi tiếp để JWT filter xử lý
        if (requestApiKey == null || requestApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Có gửi X-API-KEY nhưng sai => chặn
        if (expectedApiKey == null || !expectedApiKey.equals(requestApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"error\": \"Unauthorized\", \"message\": \"Invalid X-API-KEY\"}"
            );
            return;
        }

        // X-API-KEY đúng => cấp quyền SYSTEM
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "System_Service",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
