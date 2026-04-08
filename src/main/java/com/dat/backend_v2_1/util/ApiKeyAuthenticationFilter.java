package com.dat.backend_v2_1.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.base64-secret}")
    private String expectedApiKey;

    // QUAN TRỌNG: Hàm này quyết định Filter có được chạy hay không
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Bỏ qua nếu là request OPTIONS (Pre-flight của CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // ĐIỀU KIỆN LỌC: Nếu KHÔNG PHẢI là API check-in, thì BỎ QUA filter này (return true)
        // Khi bỏ qua, request sẽ đi thẳng tới các filter JWT/Đăng nhập mặc định của bạn
        return !path.equals("/api/v1/student-attendances/check-in");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Lúc này, Filter CHỈ chạy với đúng đường dẫn /check-in
        String requestApiKey = request.getHeader("X-API-KEY");

        if (expectedApiKey != null && expectedApiKey.equals(requestApiKey)) {
            // 1. Key Hợp Lệ: Cấp quyền giả lập để Spring Security cho đi vào Controller
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken("AI_Python_Service", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } else {
            // 2. Key Sai hoặc Thiếu: Đuổi về luôn (401)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or Missing X-API-KEY for Check-in");
        }
    }
}