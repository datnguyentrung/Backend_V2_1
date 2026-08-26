package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.error.ApiErrorResponseFactory;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final AuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    private final ApiErrorResponseFactory errorResponseFactory;

    public CustomAuthenticationEntryPoint(ApiErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @Override
    public void commence(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException, ServletException {
        this.delegate.commence(request, response, authException);
        errorResponseFactory.write(response, SecurityErrorCode.UNAUTHORIZED, request);
    }
}
