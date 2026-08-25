package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.RolePermission;
import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionMapper {
    public RolePermissionDTO.ItemResponse toResponse(RolePermission entity) {
        if (entity == null) return null;
        return new RolePermissionDTO.ItemResponse(entity.getRole().getCode(), entity.getPermission().getPermissionId(), entity.getPermission().getCode());
    }
}
