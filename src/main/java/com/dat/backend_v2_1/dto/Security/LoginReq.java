package com.dat.backend_v2_1.dto.Security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReq {
    @Data
    public static class UserBase {
//        private UUID idUser; // optional khi update

        @NotBlank(message = "Phone number must not be blank")
        private String phoneNumber;

        @NotBlank(message = "Password must not be blank")
        private String password; // raw password (sẽ mã hóa trong service)

        @NotBlank(message = "IdDevice must not be blank")
        private String idDevice;

        private String fcmToken;
    }

    @Data
    public static class RefreshRequest{
        @NotBlank(message = "RefreshToken must not be blank")
        private String refreshToken;
    }

    @Data
    public static class UpdateFcmReq{
        @NotBlank(message = "Refresh token không được để trống")
        private String refreshToken;
        // Chúng ta dùng Refresh Token làm "chìa khóa" để tìm đúng phiên đăng nhập cần update

        @NotBlank(message = "FCM Token không được để trống")
        private String fcmToken;
    }
}
