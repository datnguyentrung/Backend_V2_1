package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

public class UserRoleDTO {

    private UserRoleDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AssignRequest {
        @NotNull(message = "User id must not be null")
        UUID userId;

        @NotBlank(message = "Role code must not be blank")
        String roleCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Response {
        UUID userId;
        Set<String> roleCodes;
    }
}
