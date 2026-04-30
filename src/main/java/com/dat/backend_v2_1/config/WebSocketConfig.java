package com.dat.backend_v2_1.config;

import com.dat.backend_v2_1.socket.ClassSessionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClassSessionWebSocketHandler classSessionWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(classSessionWebSocketHandler, "/ws/class-sessions")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:4173",
                        "http://localhost:5173",
                        "https://tkdvanquan.vercel.app"
                );
    }
}
