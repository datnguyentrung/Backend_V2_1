package com.dat.backend_v2_1.controller;

import com.dat.backend_v2_1.dto.Security.LoginReq;
import com.dat.backend_v2_1.service.Security.AuthTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final AuthTokenService authTokenService;

    /**
     * API cập nhật hoặc thêm mới FCM Token gắn liền với phiên đăng nhập của User
     * FE sẽ gọi API này ngay sau khi đăng nhập thành công hoặc khi định kỳ đồng bộ Token
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/update-fcm")
    public ResponseEntity<?> updateFcmToken(@Valid @RequestBody LoginReq.UpdateFcmReq req) {
        try {
            authTokenService.updateFcmTokenOnly(req.getRefreshToken(), req.getFcmToken());
            return ResponseEntity.ok("Cập nhật FCM Token thành công");
        } catch (RuntimeException e) {
            log.error("Lỗi cập nhật FCM Token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * API xóa FCM Token khỏi hệ thống khi người dùng đăng xuất (Logout)
     */
    @DeleteMapping("/fcm-token/{token}")
    public ResponseEntity<?> deleteToken(@PathVariable String token) {
        try {
            //  BỔ SUNG: Gọi xuống tầng Service để xóa token này khỏi DB
            authTokenService.deleteFcmTokenOnly(token);

            log.info("Đã xóa FCM Token thành công khỏi DB: {}", token);
            return ResponseEntity.ok("Đã xóa token thành công khỏi hệ thống");
        } catch (Exception e) {
            log.error("Xóa token thất bại: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Xóa token thất bại: " + e.getMessage());
        }
    }
}