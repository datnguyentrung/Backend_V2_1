package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.dto.Security.LoginReq;
import com.dat.backend_v2_1.dto.Operation.NotificationDTO;
import com.dat.backend_v2_1.service.Operation.NotificationService;
import com.dat.backend_v2_1.service.Security.AuthTokenService;
import com.dat.backend_v2_1.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final AuthTokenService authTokenService;
    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<NotificationDTO.NotificationResponse> create(
            @Valid @RequestBody NotificationDTO.CreateRequest request
    ) {
        return ResponseEntity.status(201).body(notificationService.create(request));
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<NotificationDTO.NotificationResponse> getDetail(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.getDetail(notificationId));
    }

    /**
     * API cập nhật hoặc thêm mới FCM Token gắn liền với phiên đăng nhập của User
     * FE sẽ gọi API này ngay sau khi đăng nhập thành công hoặc khi định kỳ đồng bộ Token
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/update-fcm")
    public ResponseEntity<?> updateFcmToken(@Valid @RequestBody LoginReq.UpdateFcmReq req) {
        try {
            String sessionId = SecurityUtil.getCurrentSessionId()
                    .orElseThrow(() -> new RuntimeException("Missing session"));
            authTokenService.updateFcmTokenForSession(sessionId, req.getFcmToken());
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
