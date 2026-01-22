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
    }

    @Data
    public static class RefreshRequest{
        @NotBlank(message = "RefreshToken must not be blank")
        private String refreshToken;
    }
}
