package com.dat.backend_v2_1.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    // 1. Tiêm Bean FirebaseMessaging đã cấu hình ở FirebaseConfig vào đây
    @Autowired
    private FirebaseMessaging firebaseMessaging;

    // 2. Gửi cho MỘT người (Dùng cho: HLV điểm danh học viên)
    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification);

            if (data != null) {
                messageBuilder.putAllData(data);
            }

            // SỬA: Dùng trực tiếp thực thể firebaseMessaging đã tiêm vào
            String response = firebaseMessaging.send(messageBuilder.build());
            log.info("Đã gửi thông báo thành công: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("Lỗi khi gửi thông báo Firebase: ", e);
        }
    }

    // 3. Gửi cho NHIỀU người (Overload method)
    public void sendMulticastNotification(List<String> tokens, String title, String body) {
        sendMulticastNotification(tokens, title, body, null);
    }

    // 4. Gửi cho NHIỀU người với data payload
    public void sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.warn("Danh sách tokens trống, hủy gửi thông báo hàng loạt.");
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();

            if (data != null) {
                payload.putAll(data);
            }

            payload.put("title", title);
            payload.put("body", body);
            payload.put("tag", "attendance-notification");

            WebpushConfig webpushConfig = WebpushConfig.builder()
                    .putHeader("Urgency", "high")
                    .putHeader("TTL", "300")
                    .build();

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .putAllData(payload)
                    .setWebpushConfig(webpushConfig)
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

            log.info("Đã gửi thông báo hàng loạt. Thành công: {}/{}",
                    response.getSuccessCount(), tokens.size());
        } catch (FirebaseMessagingException e) {
            log.error("Lỗi khi gửi thông báo hàng loạt: ", e);
        }
    }
}