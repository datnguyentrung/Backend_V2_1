package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.UserRole;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import org.springframework.stereotype.Component;

@Component
public class UserRoleMapper {
    public UserRoleDTO.ItemResponse toResponse(UserRole entity) {
        if (entity == null) return null;
        return new UserRoleDTO.ItemResponse(entity.getUser().getUserId(), entity.getRole().getCode());
    }
}
