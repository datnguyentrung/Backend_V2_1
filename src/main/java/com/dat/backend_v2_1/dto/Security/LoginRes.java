package com.dat.backend_v2_1.dto.Security;

import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class LoginRes {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    private String idDevice;
    private UserLogin user;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserLogin {
        private String idAccount;
        private UserStatus status;
        private String role;
        private String startDate;

        @Override
        public String toString() {
            return "UserLogin{id= " + idAccount + ", status= " + status + "}";
        }
    }
}
