package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.service.Security.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    public RateLimitFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        if (requestUri.startsWith("/api/v1/auth/login") || requestUri.equals("/api/v1/persons/face-check-in")) {
            String ipAddress = getClientIP(request);
            boolean isFaceCheckIn = requestUri.equals("/api/v1/persons/face-check-in");
            Bucket bucket = isFaceCheckIn
                    ? rateLimitingService.resolveFaceCheckInBucket(ipAddress)
                    : rateLimitingService.resolveBucket(ipAddress);
            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                String message = isFaceCheckIn
                        ? "Too many face check-in requests. Please try again later."
                        : "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.";
                response.getWriter().write("{\"statusCode\":429,\"message\":\"" + message + "\",\"data\":null}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Hàm hỗ trợ lấy IP thật (rất quan trọng khi deploy thực tế)
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
