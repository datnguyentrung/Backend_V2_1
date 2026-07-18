package com.dat.backend_v2_1.dto.Security;

import com.dat.backend_v2_1.enums.Security.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRes {
    private String accessToken;
    private String idDevice;
    private UserLogin user;
    private UserContextRes activeContext;
    private List<UserContextRes> availableContexts = List.of();
    private boolean requiresContextSelection;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserLogin {
        private UUID userId;
        private String phoneNumber;
        private UserStatus status;
        private Set<String> roles;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserContextRes {
        private UUID personId;
        private String contextType;
        private String relationshipType;
        private String userCode;
        private String displayName;
    }
}
