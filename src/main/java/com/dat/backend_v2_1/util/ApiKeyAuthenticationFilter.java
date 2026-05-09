package com.dat.backend_v2_1.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.base64-secret}")
    private String expectedApiKey;

    // 🚀 BƯỚC 1: KHAI BÁO DANH SÁCH CÁC API DÙNG X-API-KEY (WEBHOOK, AI, CRON JOB...)
    private static final List<String> API_KEY_ENDPOINTS = List.of(
            "/api/v1/student-attendances/check-in",
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
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Lúc này, Filter CHỈ chạy với những API có trong danh sách API_KEY_ENDPOINTS
        String requestApiKey = request.getHeader("X-API-KEY");

        if (expectedApiKey != null && expectedApiKey.equals(requestApiKey)) {
            // 1. Key Hợp Lệ: Cấp quyền giả lập để Spring Security cho đi vào Controller
            // Chỗ này bạn có thể đổi tên thành "System_Service" cho tổng quát thay vì "AI_Python_Service" nhé
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken("System_Service", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } else {
            // 2. Key Sai hoặc Thiếu: Đuổi về luôn (401)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or Missing X-API-KEY\"}");
        }
    }
}