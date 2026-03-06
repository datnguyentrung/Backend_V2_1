//package com.dat.backend_v2_1.config;
//
//import com.google.auth.oauth2.GoogleCredentials;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.FirebaseOptions;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//@Configuration
//public class FirebaseConfig {
//
//    @Bean
//    public FirebaseApp firebaseApp() throws IOException {
//        // Kiểm tra xem Firebase đã được bật chưa (tránh lỗi khi code chạy lại)
//        if (FirebaseApp.getApps().isEmpty()) {
//
//            // Đọc file json từ thư mục resources
//            ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
//            InputStream serviceAccount = resource.getInputStream();
//
//            FirebaseOptions options = FirebaseOptions.builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                    .build();
//
//            return FirebaseApp.initializeApp(options);
//        }
//        return FirebaseApp.getInstance();
//    }
//}