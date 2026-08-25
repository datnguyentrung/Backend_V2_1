package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public final class UserDTO {
    private UserDTO() {
    }

    public record CreateRequest(@NotNull String phoneNumber, @NotNull String passwordHash, @NotNull UserStatus status, long authorizationVersion, @NotNull LocalDateTime lastLoginAt) {
    }

    public record UpdateRequest(@NotNull String phoneNumber, @NotNull String passwordHash, @NotNull UserStatus status, long authorizationVersion, @NotNull LocalDateTime lastLoginAt) {
    }

    public record Response(UUID userId, String phoneNumber, String passwordHash, UserStatus status, long authorizationVersion, LocalDateTime lastLoginAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
