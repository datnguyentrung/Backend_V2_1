package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.AuthSession;
import com.dat.ai_receptionist_web.dto.Security.AuthSessionDTO;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionMapper {
    public AuthSessionDTO.Response toResponse(AuthSession entity) {
        if (entity == null) return null;
        return new AuthSessionDTO.Response(entity.getAuthSessionId(), entity.getUser() == null ? null : entity.getUser().getUserId(), entity.getActiveUserPerson() == null ? null : entity.getActiveUserPerson().getUserPersonId(), entity.getRefreshTokenHash(), entity.getDeviceInfo(), entity.getPlatform(), entity.getFcmToken(), entity.getExpiresAt(), entity.isRevoked(), entity.getRevokedAt(), entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(AuthSessionDTO.UpdateRequest request, AuthSession entity) {
        entity.setRefreshTokenHash(request.refreshTokenHash());
        entity.setDeviceInfo(request.deviceInfo());
        entity.setPlatform(request.platform());
        entity.setFcmToken(request.fcmToken());
        entity.setExpiresAt(request.expiresAt());
        entity.setRevoked(request.revoked());
        entity.setRevokedAt(request.revokedAt());
        entity.setVersion(request.version());
    }
}
