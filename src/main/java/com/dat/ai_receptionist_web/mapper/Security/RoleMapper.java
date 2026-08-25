package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.dto.Security.RoleDTO;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {
    public Role toEntity(RoleDTO.CreateRequest request) {
        Role role = new Role();
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissionVersion(request.permissionVersion());
        return role;
    }

    public Role toEntity(RoleDTO.UpdateRequest request) {
        Role role = new Role();
        updateEntity(request, role);
        return role;
    }

    public RoleDTO.Response toResponse(Role entity) {
        if (entity == null) return null;
        return new RoleDTO.Response(entity.getCode(), entity.getName(), entity.getDescription(), entity.getPermissionVersion());
    }

    public void updateEntity(RoleDTO.UpdateRequest request, Role entity) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setPermissionVersion(request.permissionVersion());
    }
}
