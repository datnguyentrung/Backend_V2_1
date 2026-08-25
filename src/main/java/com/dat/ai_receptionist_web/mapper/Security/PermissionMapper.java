package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.dto.Security.PermissionDTO;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {
    public PermissionDTO.Response toResponse(Permission entity) {
        if (entity == null) return null;
        return new PermissionDTO.Response(entity.getPermissionId(), entity.getCode(), entity.getModel(), entity.getAction(), entity.getDescription());
    }

    public void updateEntity(PermissionDTO.UpdateRequest request, Permission entity) {
        entity.setCode(request.code());
        entity.setModel(request.model());
        entity.setAction(request.action());
        entity.setDescription(request.description());
    }
}
