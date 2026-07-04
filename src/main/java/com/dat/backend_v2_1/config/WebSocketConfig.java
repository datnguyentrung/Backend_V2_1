package com.dat.backend_v2_1.config;

import com.dat.backend_v2_1.socket.ClassSessionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClassSessionWebSocketHandler classSessionWebSocketHandler;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(classSessionWebSocketHandler, "/ws/class-sessions")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
    }
}
