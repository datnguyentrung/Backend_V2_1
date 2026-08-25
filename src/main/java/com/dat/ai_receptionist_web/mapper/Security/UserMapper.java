package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.UserDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDTO.Response toResponse(User entity) {
        if (entity == null) return null;
        return new UserDTO.Response(entity.getUserId(), entity.getPhoneNumber(), entity.getPasswordHash(), entity.getStatus(), entity.getAuthorizationVersion(), entity.getLastLoginAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(UserDTO.UpdateRequest request, User entity) {
        entity.setPhoneNumber(request.phoneNumber());
        entity.setPasswordHash(request.passwordHash());
        entity.setStatus(request.status());
        entity.setAuthorizationVersion(request.authorizationVersion());
        entity.setLastLoginAt(request.lastLoginAt());
    }
}
