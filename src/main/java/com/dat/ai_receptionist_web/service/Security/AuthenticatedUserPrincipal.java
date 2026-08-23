package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;

import java.util.List;
import java.util.UUID;

public final class AuthenticatedUserPrincipal extends org.springframework.security.core.userdetails.User {
    private final UUID userId;
    private final UserStatus status;

    public AuthenticatedUserPrincipal(User user) {
        super(user.getPhoneNumber(), user.getPasswordHash(), user.getStatus() == UserStatus.ACTIVE,
                true, true, user.getStatus() != UserStatus.LOCKED && user.getStatus() != UserStatus.BANNED,
                List.of());
        this.userId = user.getUserId();
        this.status = user.getStatus();
    }

    public UUID getUserId() { return userId; }
    public UserStatus getStatus() { return status; }
}
