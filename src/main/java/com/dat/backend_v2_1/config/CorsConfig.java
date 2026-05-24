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
        configuration.setAllowedOrigins(
                Arrays.asList(
                        "http://localhost:3000", "http://localhost:4173",
                        "http://localhost:5173", "http://localhost:5174",
                        "https://tkdvanquan.vercel.app",
                        "http://localhost:8000", "https://taekwondovanquan.vercel.app", // Đã xóa dấu / ở cuối
                        "https://datnguyentrung-ai-receptionist-be.hf.space",
                        "https://6bb8-2a09-bac5-398a-16dc-00-247-10a.ngrok-free.app"
                )
        );

        // Các phương thức HTTP được phép
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // CÁC HEADER ĐƯỢC PHÉP GỬI LÊN (Chỉ gọi 1 lần và có chứa ngrok-skip-browser-warning)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "x-no-retry", "ngrok-skip-browser-warning"));

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