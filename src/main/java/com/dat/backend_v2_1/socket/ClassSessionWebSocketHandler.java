package com.dat.backend_v2_1.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassSessionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    // Lưu tất cả các kết nối của Admin/Giáo viên đang mở trang
    private final Set<WebSocketSession> activeSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        activeSessions.add(session);
        log.info("WS: Client connected to sessions dashboard. Total: {}", activeSessions.size());
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        activeSessions.remove(session);
    }

    // Hàm này dùng để bắn tín hiệu xuống cho Frontend
    public void broadcastSessionChange(String actionType, Object data) {
        if (activeSessions.isEmpty()) return;

        try {
            Map<String, Object> message = Map.of(
                    "type", actionType, // Ví dụ: "SESSIONS_ACTIVATED" hoặc "SESSION_COMPLETED"
                    "data", data != null ? data : Map.of()
            );
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);

            for (WebSocketSession session : activeSessions) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (Exception e) {
            log.error("WS: Error broadcasting message: {}", e.getMessage());
        }
    }
}