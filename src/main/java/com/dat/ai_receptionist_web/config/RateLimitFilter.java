package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.service.Security.RateLimitingService;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import com.dat.ai_receptionist_web.error.ApiErrorResponseFactory;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitingService rateLimitingService;
    private final RateLimitProperties properties;
    private final ApiErrorResponseFactory errorResponseFactory;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        RateLimitProperties.Policy policy = properties.getPolicies().stream()
                .filter(value -> (value.getMethod() == null
                        || value.getMethod().equalsIgnoreCase(request.getMethod()))
                        && matcher.match(value.getPath(), request.getRequestURI()))
                .findFirst().orElse(null);
        if (policy == null) {
            chain.doFilter(request, response);
            return;
        }
        String subject = subject(policy.getSubject(), request);
        if (!rateLimitingService.allow(policy.getName(), subject, policy.getLimit(), policy.getWindow())) {
            errorResponseFactory.write(response, SecurityErrorCode.RATE_LIMIT_EXCEEDED, request);
            return;
        }
        chain.doFilter(request, response);
    }

    private String subject(RateLimitProperties.Subject mode, HttpServletRequest request) {
        if (mode != RateLimitProperties.Subject.IP) {
            UUID sessionId = SecurityUtil.getCurrentSessionId().orElse(null);
            if (sessionId != null) return "session:" + sessionId;
            UUID userId = SecurityUtil.getCurrentUserId().orElse(null);
            if (userId != null) return "user:" + userId;
        }
        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        if (properties.isTrustForwardedHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
