package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public final class AuthSessionDTO {
    private AuthSessionDTO() {
    }

    public record CreateRequest(@NotNull UUID userId, @NotNull UUID activeUserPersonId, @NotNull String refreshTokenHash, @NotNull String deviceInfo, @NotNull String platform, @NotNull String fcmToken, @NotNull LocalDateTime expiresAt, boolean revoked, @NotNull LocalDateTime revokedAt, long version) {
    }

    public record UpdateRequest(@NotNull UUID userId, @NotNull UUID activeUserPersonId, @NotNull String refreshTokenHash, @NotNull String deviceInfo, @NotNull String platform, @NotNull String fcmToken, @NotNull LocalDateTime expiresAt, boolean revoked, @NotNull LocalDateTime revokedAt, long version) {
    }

    public record Response(UUID authSessionId, UUID userId, UUID activeUserPersonId, String refreshTokenHash, String deviceInfo, String platform, String fcmToken, LocalDateTime expiresAt, boolean revoked, LocalDateTime revokedAt, long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
