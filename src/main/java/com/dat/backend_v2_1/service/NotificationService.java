package com.dat.backend_v2_1.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j // Tự động tạo logger để in log
public class NotificationService {

    // 1. Gửi cho MỘT người (Dùng cho: HLV điểm danh học viên)
    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        try {
            // Xây dựng thông báo
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Xây dựng tin nhắn
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification);

            // Nếu có dữ liệu kèm theo (ví dụ: id của buổi tập để click vào xem chi tiết)
            if (data != null) {
                messageBuilder.putAllData(data);
            }

            // Gửi đi
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("Đã gửi thông báo thành công: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("Lỗi khi gửi thông báo Firebase: ", e);
        }
    }

    // 2. Gửi cho NHIỀU người (Dùng cho: Admin thông báo cả lớp)
    public void sendMulticastNotification(List<String> tokens, String title, String body) {
        sendMulticastNotification(tokens, title, body, null);
    }

    // 3. Gửi cho NHIỀU người với data payload (Dùng cho: Điểm danh, đánh giá với navigation data)
    public void sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return;

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(notification);

            // Nếu có dữ liệu kèm theo (ví dụ: screen name để navigation)
            if (data != null) {
                messageBuilder.putAllData(data);
            }

            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(messageBuilder.build());
            log.info("Đã gửi thông báo cho {} thiết bị.", response.getSuccessCount());

        } catch (FirebaseMessagingException e) {
            log.error("Lỗi khi gửi thông báo hàng loạt: ", e);
        }
    }
}