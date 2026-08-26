package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.service.Security.AuthorizationService;
import com.dat.ai_receptionist_web.error.ApiErrorResponseFactory;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccessTokenStateValidationFilter extends OncePerRequestFilter {
    private final AuthorizationService authorizationService;
    private final ApiErrorResponseFactory errorResponseFactory;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            try {
                authorizationService.validateAccessToken(jwt);
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
                errorResponseFactory.write(response, SecurityErrorCode.TOKEN_STALE, request);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
