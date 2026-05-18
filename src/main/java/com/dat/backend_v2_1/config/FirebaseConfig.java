package com.dat.backend_v2_1.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    // Spring Boot tự động bốc dữ liệu từ application.yml ném vào biến này
    @Value("${firebase.config-base64}")
    private String base64Config;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount;

            // Kiểm tra xem biến cấu hình lấy từ file .yml có dữ liệu không
            if (base64Config != null && !base64Config.isEmpty()) {
                // Môi trường Deploy trên Render (Đã cấu hình biến env)
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Config.trim());
                serviceAccount = new java.io.ByteArrayInputStream(decodedBytes);
                System.out.println("🔥 [Firebase] Khởi tạo thành công từ YML Base64!");
            } else {
                // Môi trường chạy dưới máy Local (Dùng file json trong resources)
                ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                serviceAccount = resource.getInputStream();
                System.out.println("🔥 [Firebase] Khởi tạo thành công từ file local JSON!");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    // BỔ SUNG THÊM: Tạo Bean cho FirebaseMessaging
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}