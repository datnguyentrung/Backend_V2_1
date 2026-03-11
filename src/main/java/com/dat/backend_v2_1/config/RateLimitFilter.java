package com.dat.backend_v2_1.config;

import com.dat.backend_v2_1.service.Security.RateLimitingService;
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
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Chỉ áp dụng Rate Limit cho API đăng nhập
        if (request.getRequestURI().startsWith("/api/v1/auth/login")) {

            // Lấy địa chỉ IP của người dùng (Xử lý cả trường hợp qua Proxy/Nginx)
            String ipAddress = getClientIP(request);

            // Lấy xô nước của IP này
            Bucket bucket = rateLimitingService.resolveBucket(ipAddress);

            // tryConsume(1) -> Cố gắng lấy 1 giọt nước. Nếu trả về true là còn nước.
            if (!bucket.tryConsume(1)) {
                // HẾT NƯỚC -> Chặn lại và báo lỗi 429
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"statusCode\": 429, \"message\": \"Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.\", \"data\": null}");
                return; // Kết thúc request tại đây, không cho đi tiếp vào Controller!
            }
        }

        // Còn nước hoặc không phải API login -> Cho phép đi tiếp
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