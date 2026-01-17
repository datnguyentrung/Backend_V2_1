package com.dat.backend_v2_1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Cho phép frontend gọi API
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:4173",
                "http://localhost:5173", "https://tkdvanquan.vercel.app"));
        // Các phương thức HTTP được phép
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Các header được phép gửi lên
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "x-no-retry"));
        // Cho phép gửi cookie, JWT,...
        configuration.setAllowCredentials(true);
        // Cache pre-flight request trong 1 giờ
        configuration.setMaxAge(3600L);
        // Áp dụng cho tất cả endpoint
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
