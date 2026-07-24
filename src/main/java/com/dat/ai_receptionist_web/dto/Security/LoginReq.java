package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReq {
    @Data
    public static class UserBase {
        @NotBlank(message = "Phone number must not be blank")
        private String phoneNumber;

        @NotBlank(message = "Password must not be blank")
        private String password;

        @NotBlank(message = "IdDevice must not be blank")
        private String idDevice;

        private String fcmToken;
    }

    @Data
    public static class RefreshRequest {
    }

    @Data
    public static class UpdateFcmReq {
        @NotBlank(message = "FCM token must not be blank")
        private String fcmToken;
    }

    @Data
    public static class SwitchContextReq {
        @NotBlank(message = "Person id must not be blank")
        private String personId;

        @NotBlank(message = "Context type must not be blank")
        private String contextType;
    }
}
