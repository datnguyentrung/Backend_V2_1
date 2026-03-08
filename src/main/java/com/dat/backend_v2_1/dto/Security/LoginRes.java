package com.dat.backend_v2_1.dto.Security;

import com.dat.backend_v2_1.enums.Security.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
public class LoginRes {
    private String accessToken;

    private String refreshToken;

    private String idDevice;
    private UserLogin user;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserLogin {
        private UUID userId;
        private UserStatus status;
        private String role;
        private String startDate;

        @Override
        public String toString() {
            return "UserLogin{id= " + userId + ", status= " + status + "}";
        }
    }
}
